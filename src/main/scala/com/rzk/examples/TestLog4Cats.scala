package com.rzk.examples

import cats.effect.kernel.Sync
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object TestLog4Cats extends IOApp {

  class LoggerUsingService[F[_]: Sync] {
    implicit val logFactory: LoggerFactory[F] = Slf4jFactory.create[F]

    def use(args: String): F[Unit] = {
      val logger = logFactory.getLogger
      for {
        _ <- logger.info("This works! Yeah!")
        _ <- logger.debug(s"and $args")
      } yield ()
    }
  }

  override def run(args: List[String]): IO[ExitCode] =
    new LoggerUsingService[IO].use(args.mkString("Array(", ",", ")")).as(ExitCode.Success)

}
