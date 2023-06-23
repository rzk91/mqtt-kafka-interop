package com.rzk.services

import cats.effect._
import com.rzk._
import com.rzk.common.Finding

object Kafka2Mqtt extends MqttKafkaService[IO, Finding] with IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    kafka2MqttService()
      .as(ExitCode.Success)
      .orElse(IO.pure(ExitCode.Error))
}
