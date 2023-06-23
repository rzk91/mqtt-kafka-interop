package com.rzk.examples

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.comcast.ip4s._
import fs2.Stream
import net.sigusr.mqtt.api.QualityOfService.{AtLeastOnce, AtMostOnce, ExactlyOnce}
import net.sigusr.mqtt.api._

import scala.concurrent.duration._
import scala.util.Random

object LocalPublisher extends IOApp {

  private val localPublisher: String = "test_pub"
  private val random: Stream[IO, Int] = Stream.eval(IO(Math.abs(Random.nextInt()))).repeat

  private val topics =
    Stream(("AtMostOnce", AtMostOnce), ("AtLeastOnce", AtLeastOnce), ("ExactlyOnce", ExactlyOnce)).repeat

  override def run(args: List[String]): IO[ExitCode] =
    if (args.nonEmpty) {
      val messages = args.toVector
      val transportConfig =
        TransportConfig[IO](
          ipv4"0.0.0.0",
          port"1883",
          traceMessages = true
        )
      val sessionConfig = SessionConfig(localPublisher, keepAlive = 5)

      Session[IO](transportConfig, sessionConfig)
        .use { session =>
          val sessionStatus = session.state.discrete
            .evalMap(logSessionStatus[IO])
            .evalMap(onSessionError[IO])
            .compile
            .drain
          val publisher = (for {
            (message, (topic, qos)) <- ticks.zipRight(randomMessage(messages).zip(topics))
            _ <- Stream.eval(
              printLine[IO](
                s"Publishing on topic ${scala.Console.CYAN}$topic${scala.Console.RESET} with QoS " +
                s"${scala.Console.CYAN}${qos.show}${scala.Console.RESET} message ${scala.Console.BOLD}$message${scala.Console.RESET}"
              )
            )
            _ <- Stream.eval(session.publish(topic, payload(message), qos))
          } yield ()).compile.drain

          for {
            _ <- IO.race(publisher, sessionStatus)
          } yield ExitCode.Success
        }
        .handleErrorWith(_ => IO.pure(ExitCode.Error))
    } else
      printLine[IO](s"${scala.Console.RED}At least one or more « messages » should be provided.${scala.Console.RESET}")
        .as(ExitCode.Error)

  private def ticks: Stream[IO, Unit] =
    random.flatMap { r =>
      val interval = r % 2000 + 1000
      Stream.sleep[IO](interval.milliseconds)
    }

  private def randomMessage(messages: Vector[String]): Stream[IO, String] =
    random.flatMap(r => Stream.emit(messages(r % messages.length)))
}
