package com.rzk.common

import io.circe._
import io.circe.syntax._

case class Event(timestamp: Long, deviceId: String, content: Json) {
  override def toString: String = s"Event($timestamp, $deviceId, ${content.noSpaces})"
}

object Event {

  implicit val decodeEvent: Decoder[Event] = (c: HCursor) =>
    for {
      timestamp <- c.downField("timestamp").as[Long]
      deviceId  <- c.downField("deviceId").as[String]
      content   <- c.downField("content").as[Json]
    } yield Event(timestamp, deviceId, content)

  implicit val encodeEvent: Encoder[Event] = (e: Event) =>
    Map(
      "timestamp" -> e.timestamp.asJson,
      "deviceId"  -> e.deviceId.asJson,
      "content"   -> e.content.noSpaces.asJson
    ).asJson
}
