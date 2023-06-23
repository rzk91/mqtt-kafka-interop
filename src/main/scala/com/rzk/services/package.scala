package com.rzk

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.show._
import fs2.kafka._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import net.sigusr.mqtt.api.ConnectionState
import net.sigusr.mqtt.api.ConnectionState._
import net.sigusr.mqtt.api.Errors._
import org.typelevel.log4cats.SelfAwareStructuredLogger

import scala.util.Try

package object services {

  val payload: String => Vector[Byte] = (_: String).getBytes("UTF-8").toVector

  val payloadAsString: Vector[Byte] => Either[Throwable, String] = v =>
    Try(v.toArray).flatMap(ab => Try(new String(ab, "UTF-8"))).toEither

  def logSessionStatus[F[_]](s: ConnectionState, logger: SelfAwareStructuredLogger[F])(implicit
    F: Sync[F]
  ): F[ConnectionState] = {
    s match {
      case Error(ConnectionFailure(reason)) =>
        logger.error(s"${Console.RED}${reason.show}${Console.RESET}")
      case Error(ProtocolError) =>
        logger.error(s"${Console.RED}Protocol error${Console.RESET}")
      case Disconnected =>
        logger.warn(s"${Console.BLUE}Transport disconnected${Console.RESET}")
      case Connecting(nextDelay, retriesSoFar) =>
        logger.warn(
          s"${Console.BLUE}Transport connecting. $retriesSoFar attempt(s) so far, next attempt in $nextDelay ${Console.RESET}"
        )
      case Connected =>
        logger.info(s"${Console.BLUE}Transport connected${Console.RESET}")
      case SessionStarted =>
        logger.info(s"${Console.BLUE}Session started${Console.RESET}")
    }
  } >> F.pure(s)

  def onSessionError[F[_]](s: ConnectionState)(implicit F: Sync[F]): F[Unit] = s match {
    case Error(e) => F.raiseError(e)
    case _        => F.unit
  }

  implicit def deserializer[F[_], A: Decoder](implicit
    F: Sync[F]
  ): Deserializer[F, Either[Throwable, A]] =
    Deserializer.lift(bytes => F.fromEither(decode[A](new String(bytes)))).attempt

  implicit def serializer[F[_], A: Encoder](implicit F: Sync[F]): Serializer[F, A] =
    Serializer.lift(j => F.pure(j.asJson.noSpaces.getBytes))
}
