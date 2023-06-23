package com.rzk.services

import cats.Applicative
import cats.effect.implicits._
import cats.effect.kernel.Async
import cats.effect.std.Console
import cats.syntax.all._
import com.rzk.runconfig.RunConfiguration
import com.rzk.runconfig.RunConfiguration.Config
import fs2.Stream
import fs2.concurrent.SignallingRef
import fs2.kafka._
import io.circe._
import io.circe.syntax._
import net.sigusr.mqtt.api.QualityOfService.{AtMostOnce, ExactlyOnce}
import net.sigusr.mqtt.api.{Message, QualityOfService}
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

abstract class MqttKafkaService[F[_]: Async: Console: LoggerFactory, V: Encoder: Decoder]
    extends MqttService[F]
    with KafkaService[F] {

  def processMessages(
    message: Message,
    mainTopic: String,
    stopTopic: Option[String],
    stopSignal: SignallingRef[F, Boolean],
    logger: SelfAwareStructuredLogger[F]
  ): Stream[F, V] = Stream.empty

  final protected def mqtt2KafkaService(): F[Unit] =
    mqttSessionResourceWithLogger(RunConfiguration.config[F], subscriber = true)
      .use { case (session, Config(mqtt, kafka), logger) =>
        val stopTopic = mqtt.topics.stop
        val subscribedTopics =
          Vector((mqtt.topics.incoming, AtMostOnce)) ++
          stopTopic.fold(Vector.empty[(String, QualityOfService)])(s => Vector((s, ExactlyOnce)))

        SignallingRef[F, Boolean](false)
          .flatMap { stopSignal =>
            val sessionStatus = mqttSessionStatusFor(session, logger, Some(stopSignal))
            val subscriber = mqttSubscriberFor(subscribedTopics, session, stopSignal, logger)

            val kafkaProducer = {
              for {
                producer <- KafkaProducer.stream[F, Unit, V](kafkaProducerSettings(kafka))
                _        <- Stream.eval(logger.info("Started Kafka producer..."))
                message  <- session.messages
                _ <- Stream.eval(
                  logger.trace(s"Incoming message on topic '${message.topic}': ${payloadAsString(message.payload)}")
                )
                input <- processMessages(message, mqtt.topics.incoming, stopTopic, stopSignal, logger)
                _     <- Stream.eval(logger.debug(s"Sending to Kafka topic '${kafka.topics.outgoing}': $input"))
                record = ProducerRecord(kafka.topics.outgoing, (), input)
                _ <- Stream.eval(producer.produce(ProducerRecords.one(record)).flatten)
              } yield ()
            }.interruptWhen(stopSignal)
              .compile
              .drain
              .onCancel(logger.info("Shutting down Kafka producer..."))

            sessionStatus.parProduct(subscriber.race(kafkaProducer)) >> A.unit
          }
      }

  final protected def kafka2MqttService(): F[Unit] =
    mqttSessionResourceWithLogger(RunConfiguration.config[F], subscriber = false)
      .use { case (session, Config(mqtt, kafka), logger) =>
        val sessionStatus = mqttSessionStatusFor(session, logger)

        val kafkaConsumer = {
          for {
            consumer <- KafkaConsumer
              .stream[F, Unit, Either[Throwable, V]](kafkaConsumerSettings(kafka))
              .subscribeTo(kafka.topics.incoming)
            _ <- Stream.eval(logger.info(s"Started Kafka consumer for topic '${kafka.topics.incoming}'..."))
            (offset, output) <- consumer.records.evalMapFilter[F, (CommittableOffset[F], V)](r =>
              r.record.value match {
                case Left(value) =>
                  logger.warn(s"Parsing error while deserializing finding: $value") >>
                    A.pure(none[(CommittableOffset[F], V)])
                case Right(value) => A.pure((r.offset, value).some)
              }
            )
            _ <- Stream.eval(logger.debug(s"Publishing on MQTT topic '${mqtt.topics.outgoing}': $output"))
            _ <- Stream.eval(session.publish(mqtt.topics.outgoing, payload(output.asJson.noSpaces), ExactlyOnce))
          } yield offset
        }
          .through(commitBatchWithin(kafka.checkpointing.chunkSize, kafka.checkpointing.interval))
          .compile
          .drain
          .onCancel(logger.info("Shutting down Kafka consumer..."))

        sessionStatus.race(kafkaConsumer) >> A.unit
      }

  private val A: Applicative[F] = Applicative[F]
}
