package com

import cats.effect.Sync
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

package object rzk {
  implicit def logging[F[_]: Sync]: LoggerFactory[F] = Slf4jFactory.create[F]
}
