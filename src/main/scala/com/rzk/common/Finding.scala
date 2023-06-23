package com.rzk.common

import com.rzk.utils.Severity.Severity
import io.circe._
import io.circe.generic.semiauto._

case class Finding(
  botName: String,
  deviceId: String,
  selector: String,
  start: Long,
  end: Long,
  severity: Severity,
  title: String,
  description: String
)

object Finding {

  implicit val decodeFinding: Decoder[Finding] = deriveDecoder
  implicit val encodeFinding: Encoder[Finding] = deriveEncoder
}
