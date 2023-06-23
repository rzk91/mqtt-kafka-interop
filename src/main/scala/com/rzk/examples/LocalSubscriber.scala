package com.rzk.examples

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import fs2.Stream
import fs2.concurrent.SignallingRef
import io.circe.generic.auto._
import io.circe.parser._
import net.sigusr.mqtt.api.QualityOfService.{AtMostOnce, ExactlyOnce}
import net.sigusr.mqtt.api.RetryConfig.Predefined
import net.sigusr.mqtt.api._

import scala.concurrent.duration._

object LocalSubscriber extends IOApp {

  private val localSubscriber: String = "test_sub"
  private val stopTopic: String = "test/stop"
  private val sensorReadingTopic: String = "test/reading"
  private val otherTopic: String = "test/other"

  private val subscribedTopics: Vector[(String, QualityOfService)] = Vector(
    stopTopic          -> ExactlyOnce,
    sensorReadingTopic -> AtMostOnce,
    otherTopic         -> AtMostOnce
  )

  override def run(args: List[String]): IO[ExitCode] = {
    val transportConfig = TransportConfig[IO](
      ipv4"0.0.0.0",
      port"1883",
      retryConfig = Predefined[IO](policy = PredefinedRetryPolicy.ConstantDelay, maxRetries = 3, baseDelay = 2.seconds),
      traceMessages = true
    )

    val sessionConfig = SessionConfig(localSubscriber, keepAlive = 10)

    Session[IO](transportConfig, sessionConfig)
      .use { session =>
        SignallingRef[IO, Boolean](false)
          .flatMap { stopSignal =>
            val sessionStatus = session.state.discrete
              .evalMap(logSessionStatus[IO])
              .evalMap(onSessionError[IO])
              .interruptWhen(stopSignal)
              .compile
              .drain

            val subscriber = for {
              s <- session.subscribe(subscribedTopics)
              _ <- s.traverse { p =>
                printLine[IO](
                  s"Topic ${scala.Console.CYAN}${p._1}${scala.Console.RESET} subscribed with QoS " +
                  s"${scala.Console.CYAN}${p._2.show}${scala.Console.RESET}"
                )
              }
              _ <- stopSignal.discrete.compile.drain
            } yield ()

            val reader = session.messages.flatMap(processMessages(stopSignal)).interruptWhen(stopSignal).compile.drain

            sessionStatus.parProduct(subscriber.race(reader)) >> IO.unit
          }
          .asInstanceOf[IO[Boolean]]
      }
      .as(ExitCode.Success)
      .handleErrorWith(_ => IO(ExitCode.Error))
  }

  def processMessages(stopSignal: SignallingRef[IO, Boolean]): Message => Stream[IO, Unit] = {
    case Message(LocalSubscriber.stopTopic, _) => Stream.exec(stopSignal.set(true))
    case Message(LocalSubscriber.sensorReadingTopic, payload) =>
      Stream
        .fromEither[IO](decode[Reading](new String(payload.toArray, "UTF-8")))
        .evalMap(r =>
          printLine[IO](
            s"Topic ${scala.Console.CYAN}$sensorReadingTopic${scala.Console.RESET}: " +
            s"${scala.Console.BOLD}$r${scala.Console.RESET}"
          )
        )
    case Message(topic, payload) =>
      Stream.eval(
        printLine[IO](
          s"Topic ${scala.Console.CYAN}$topic${scala.Console.RESET}: " +
          s"${scala.Console.BOLD}${new String(payload.toArray, "UTF-8")}${scala.Console.RESET}"
        )
      )
  }

  case class Reading(sensor: String, value: Double)
}
