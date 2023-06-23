package com.rzk

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.show._
import net.sigusr.mqtt.api.ConnectionState
import net.sigusr.mqtt.api.ConnectionState._
import net.sigusr.mqtt.api.Errors._

package object examples {

  val payload: String => Vector[Byte] = (_: String).getBytes("UTF-8").toVector

  def logSessionStatus[F[_]](s: ConnectionState)(implicit F: Sync[F]): F[ConnectionState] = {
    s match {
      case Error(ConnectionFailure(reason)) =>
        printLine(s"${Console.RED}${reason.show}${Console.RESET}")
      case Error(ProtocolError) =>
        printLine(s"${Console.RED}Protocol error${Console.RESET}")
      case Disconnected =>
        printLine(s"${Console.BLUE}Transport disconnected${Console.RESET}")
      case Connecting(nextDelay, retriesSoFar) =>
        printLine(
          s"${Console.BLUE}Transport connecting. $retriesSoFar attempt(s) so far, next attempt in $nextDelay ${Console.RESET}"
        )
      case Connected =>
        printLine(s"${Console.BLUE}Transport connected${Console.RESET}")
      case SessionStarted =>
        printLine(s"${Console.BLUE}Session started${Console.RESET}")
    }
  } >> F.pure(s)

  def printLine[F[_]](s: String)(implicit F: Sync[F]): F[Unit] = F.delay(println(s))

  def onSessionError[F[_]](s: ConnectionState)(implicit F: Sync[F]): F[Unit] = s match {
    case Error(e) => F.raiseError(e)
    case _        => F.unit
  }
}
