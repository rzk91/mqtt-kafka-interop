package com.rzk.runconfig

import cats.Applicative
import cats.effect.Sync
import cats.effect.kernel.Resource
import cats.implicits._
import com.comcast.ip4s.{Host, Port}
import com.rzk.utils.SinkStatus
import com.rzk.utils.implicits.enrichTime
import net.sigusr.mqtt.api.RetryConfig.Predefined
import net.sigusr.mqtt.api.{PredefinedRetryPolicy, SessionConfig, TransportConfig}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.typelevel.log4cats.LoggerFactory
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

import java.util.Properties
import scala.concurrent.duration.FiniteDuration

object RunConfiguration {

  def config[F[_]: Sync: LoggerFactory]: F[Config] = {
    val logger = LoggerFactory[F].getLogger
    for {
      conf <- source.loadF[F, Config]()
      _    <- logger.info(s"$conf")
    } yield conf
  }

  def configAsResource[F[_]: Sync: LoggerFactory]: Resource[F, Config] = Resource.eval(config)

  case class Config(mqtt: MqttConfig, kafka: KafkaConfig)

  case class ClientId(producer: String, subscriber: String)
  case class Topics(incoming: String, outgoing: String, stop: Option[String])

  case class Session(user: String, password: String, groupIdPrefix: String, clientIdSuffix: ClientId, keepAlive: Int) {

    def simpleSessionConfig(subscriber: Boolean): SessionConfig = SessionConfig(
      s"$groupIdPrefix-${if (subscriber) clientIdSuffix.subscriber else clientIdSuffix.producer}",
      keepAlive = keepAlive,
      user = Some(user),
      password = Some(password)
    )
  }

  case class RetryConfig(policy: PredefinedRetryPolicy, maxRetries: Int, baseDelay: FiniteDuration) {
    def predefined[F[_]: Applicative]: Predefined[F] = Predefined(policy, maxRetries, baseDelay)
  }

  case class MqttConfig(
    host: Host,
    port: Port,
    session: Session,
    retry: RetryConfig,
    topics: Topics,
    trace: Boolean,
    sinkStatus: SinkStatus
  ) {

    def simpleTransportConfig[F[_]: Applicative]: TransportConfig[F] = TransportConfig(
      host = host,
      port = port,
      retryConfig = retry.predefined[F],
      traceMessages = trace
    )
  }

  case class KafkaAuth(required: Boolean, securityProtocol: String, saslMechanism: String, jaasConfig: String) {

    def asProperties: Properties = {
      val prop = new Properties()
      if (required) {
        prop.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol)
        prop.setProperty(SaslConfigs.SASL_MECHANISM, saslMechanism)
        prop.setProperty(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig)
      }

      prop
    }
  }

  case class KafkaCheckpointing(chunkSize: Int, interval: FiniteDuration)

  case class KafkaConfig(
    bootstrapServers: List[String],
    auth: KafkaAuth,
    groupIdPrefix: String,
    topics: Topics,
    maxBatch: Int,
    checkpointing: KafkaCheckpointing,
    sinkStatus: SinkStatus
  ) {

    def groupId(consumer: Boolean): String = s"$groupIdPrefix-kafka-${if (consumer) "reader" else "writer"}"

    def additionalProducerProperties: Properties = {
      val props = new Properties()
      props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true)
      props.put(ProducerConfig.ACKS_CONFIG, "all")
      props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE)
      props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1)
      props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10.seconds)
      props.put(ProducerConfig.LINGER_MS_CONFIG, 5.seconds)
      props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 17.seconds)
      props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "zstd")
      props.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 60.seconds)
      props
    }
  }

}
