package com.rzk.services

import cats.effect.Sync
import com.rzk.runconfig.RunConfiguration.KafkaConfig
import com.rzk.utils.implicits.enrichProperties
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, IsolationLevel, ProducerSettings}
import io.circe._

trait KafkaService[F[_]] {

  protected def kafkaProducerSettings[IN](
    kafkaConfig: KafkaConfig
  )(implicit encoder: Encoder[IN], sync: Sync[F]): ProducerSettings[F, Unit, IN] =
    ProducerSettings[F, Unit, IN]
      .withBootstrapServers(kafkaConfig.bootstrapServers.mkString(","))
      .withClientId(kafkaConfig.groupId(consumer = false))
      .withProperties(kafkaConfig.auth.asProperties.toMap)
      .withProperties(kafkaConfig.additionalProducerProperties.toMap)

  protected def kafkaConsumerSettings[OUT](
    kafkaConfig: KafkaConfig
  )(implicit decoder: Decoder[OUT], sync: Sync[F]): ConsumerSettings[F, Unit, Either[Throwable, OUT]] =
    ConsumerSettings[F, Unit, Either[Throwable, OUT]]
      .withIsolationLevel(IsolationLevel.ReadCommitted)
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(kafkaConfig.bootstrapServers.mkString(","))
      .withProperties(kafkaConfig.auth.asProperties.toMap)
      .withGroupId(kafkaConfig.groupId(consumer = true))
      .withMaxPollRecords(kafkaConfig.maxBatch)
}
