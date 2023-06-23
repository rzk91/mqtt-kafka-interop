package com.rzk.utils

import io.circe._

object Severity extends Enumeration {
  type Severity = Value
  val INFO, LOW, MEDIUM, HIGH = Value

  implicit val severityEncoder: Encoder[Severity] = Encoder.encodeEnumeration(Severity)
  implicit val severityDecoder: Decoder[Severity] = Decoder.decodeEnumeration(Severity)
}
