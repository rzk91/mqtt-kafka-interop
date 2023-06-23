package com.rzk.utils

import cats.Show
import enumeratum.values._

sealed abstract class SinkStatus(val value: String) extends StringEnumEntry

object SinkStatus extends StringEnum[SinkStatus] {
  val values: IndexedSeq[SinkStatus] = findValues

  case object Active extends SinkStatus("ACTIVE")
  case object Debug extends SinkStatus("DEBUG")
  case object Testing extends SinkStatus("TESTING")
  case object Disabled extends SinkStatus("DISABLED")

  implicit class SinkStatusOps(private val status: String) extends AnyVal {
    def toSinkStatus: Option[SinkStatus] = withValueOpt(status.toUpperCase)
  }

  implicit val showSinkStatus: Show[SinkStatus] = Show.show(_.value)
}
