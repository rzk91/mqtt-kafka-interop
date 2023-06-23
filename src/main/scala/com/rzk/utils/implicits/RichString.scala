package com.rzk.utils.implicits

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.ZoneId
import java.util.UUID
import scala.util.Try

class RichString(private val str: String) extends AnyVal {
  // Safe conversion methods
  def asInt: Option[Int] = Try(str.trim.toInt).toOption
  def asLong: Option[Long] = Try(str.trim.toLong).toOption
  def asDouble: Option[Double] = Try(str.toDouble).toOption

  def asBoolean: Option[Boolean] =
    Try(str.trim.toLowerCase).collect {
      case v: String if v == "true" || v == "1"  => true
      case v: String if v == "false" || v == "0" => false
    }.toOption

  // Other helper methods
  def toOption: Option[String] = if (str.isBlank) None else Some(str)
  def hasValue: Boolean = toOption.isDefined
  def trimEdges: String = str.tail.init
  def validRegex: Boolean = Try(str.r).isSuccess
  def removeSubstring(substr: String): String = str.replace(substr, "")
  def removeSeparator(sep: Char): String = str.replaceAll(escape(sep), "")
  def removeQuotes(): String = removeSeparator('"')
  def removeSpaces(): String = removeSeparator(' ')
  def fixDecimalNotation(sep: Char = ','): String = str.trim.replaceAll(escape(sep), ".")
  def deToEnNotation(): String = removeSeparator('.').fixDecimalNotation()
  def encodeSpacesInUrl(): String = str.replaceAll(" ", "%20")
  def encodePlusInUrl(): String = str.replaceAll("\\+", "%2B")
  def elseIfBlank(default: => String): String = if (str.isBlank) default else str

  def elseIfContains(substr: String, default: => String): String =
    if (str.contains(substr)) default else str

  def splitInTwo(by: Char): (String, String) = {
    val split = str.split(by)
    (split.head, split.last)
  }
  def zoneIdOption: Option[ZoneId] = Try(ZoneId.of(str)).toOption
  def zoneId(default: => ZoneId = ZoneId.of("UTC")): ZoneId = zoneIdOption.getOrElse(default)

  private def escape(char: Char): String = {
    val W = "(\\w)".r
    char match {
      case W(_) => char.toString
      case _    => s"\\$char"
    }
  }

  // Uuid utils
  def toUuid: String = {
    val digester = MessageDigest.getInstance("SHA-1")
    digester.update(str.getBytes(StandardCharsets.UTF_8))
    val digest = digester.digest
    digest(6) = (digest(6) & 15 | 5 << 4).toByte // clear version and set it to 5 (name-based sha1)
    digest(8) = (digest(8) & 63 | 128).toByte    // clear variant and set it to IETF variant

    new UUID(
      (digest(0) << 24 | (digest(1) & 255) << 16 | (digest(2) & 255) << 8 | digest(3) & 255).toLong << 32
      | (digest(4) << 24 | (digest(5) & 255) << 16 | (digest(6) & 255) << 8 | digest(7) & 255).toLong << 32 >>> 32,
      (digest(8) << 24 | (digest(9) & 255) << 16 | (digest(10) & 255) << 8 | digest(11) & 255).toLong << 32
      | (digest(12) << 24 | (digest(13) & 255) << 16 | (digest(14) & 255) << 8 | digest(15) & 255).toLong << 32 >>> 32
    ).toString

  }
}
