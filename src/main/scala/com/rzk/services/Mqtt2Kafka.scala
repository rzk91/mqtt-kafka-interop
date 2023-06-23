package com.rzk.services

import cats.effect._
import com.rzk._
import com.rzk.common.Event
import fs2.Stream
import fs2.concurrent.SignallingRef
import io.circe.parser._
import net.sigusr.mqtt.api.Message
import org.typelevel.log4cats.SelfAwareStructuredLogger

object Mqtt2Kafka extends MqttKafkaService[IO, Event] with IOApp {

  override def processMessages(
    message: Message,
    mainTopic: String,
    stopTopic: Option[String],
    stopSignal: SignallingRef[IO, Boolean],
    logger: SelfAwareStructuredLogger[IO]
  ): Stream[IO, Event] =
    message match {
      case Message(`mainTopic`, payload) =>
        payloadAsString(payload).flatMap(decode[Event]) match {
          case Left(exception) =>
            Stream.eval(logger.warn(s"Invalid payload, got exception: $exception")) >> Stream.empty
          case Right(value) => Stream.emit(value)
        }
      case Message(topic, _) if stopTopic.contains(topic) => Stream.eval(stopSignal.set(true)) >> Stream.empty
      case _                                              => Stream.empty
    }

  override def run(args: List[String]): IO[ExitCode] =
    mqtt2KafkaService()
      .as(ExitCode.Success)
      .orElse(IO.pure(ExitCode.Error))
}
