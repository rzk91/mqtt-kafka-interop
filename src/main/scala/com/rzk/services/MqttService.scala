package com.rzk.services

import cats.effect.Resource
import cats.effect.kernel.Async
import cats.effect.std.Console
import cats.syntax.all._
import com.rzk.runconfig.RunConfiguration.Config
import fs2.concurrent.SignallingRef
import net.sigusr.mqtt.api.{QualityOfService, Session}
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

trait MqttService[F[_]] {

  protected def mqttSessionResource(fc: F[Config], subscriber: Boolean)(implicit
    async: Async[F],
    console: Console[F]
  ): Resource[F, (Session[F], Config)] =
    for {
      conf <- Resource.eval(fc)
      mqtt = conf.mqtt
      tc = mqtt.simpleTransportConfig[F]
      sc = mqtt.session.simpleSessionConfig(subscriber)
      session <- Session[F](tc, sc)
    } yield (session, conf)

  protected def mqttSessionResourceWithLogger(
    fc: F[Config],
    subscriber: Boolean
  )(implicit
    async: Async[F],
    console: Console[F],
    loggerFactory: LoggerFactory[F]
  ): Resource[F, (Session[F], Config, SelfAwareStructuredLogger[F])] =
    mqttSessionResource(fc, subscriber).map { case (session, config) =>
      (session, config, loggerFactory.getLogger)
    }

  protected def mqttSessionStatusFor(
    session: Session[F],
    logger: SelfAwareStructuredLogger[F],
    stopSignal: Option[SignallingRef[F, Boolean]] = None
  )(implicit async: Async[F]): F[Unit] = {
    val s = session.state.discrete
      .evalMap(logSessionStatus[F](_, logger))
      .evalMap(onSessionError[F])

    stopSignal.fold(s)(s.interruptWhen(_)).compile.drain
  }

  protected def mqttSubscriberFor(
    subscribedTopics: Vector[(String, QualityOfService)],
    session: Session[F],
    stopSignal: SignallingRef[F, Boolean],
    logger: SelfAwareStructuredLogger[F]
  )(implicit async: Async[F]): F[Unit] =
    for {
      s <- session.subscribe(subscribedTopics)
      _ <- s.traverse { case (topic, qos) =>
        logger.info(
          s"Topic ${scala.Console.CYAN}$topic${scala.Console.RESET} subscribed with QoS " +
          s"${scala.Console.CYAN}${qos.show}${scala.Console.RESET}"
        )
      }
      _ <- stopSignal.discrete.compile.drain
      _ <- logger.warn("Closing subscriber after receiving stop signal")
    } yield ()
}
