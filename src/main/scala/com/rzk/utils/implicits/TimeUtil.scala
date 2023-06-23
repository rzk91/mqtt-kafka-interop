package com.rzk.utils.implicits

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId, ZonedDateTime}
import scala.util.{Failure, Success, Try}

object TimeUtil extends Enumeration {

  type TimeUnit = Value

  val S: Value = Value("S")
  val SEC: Value = Value("SEC")
  val SECOND: Value = Value("SECOND")
  val SECONDS: Value = Value("SECONDS")

  val M: Value = Value("M")
  val MIN: Value = Value("MIN")
  val MINUTE: Value = Value("MINUTE")
  val MINUTES: Value = Value("MINUTES")

  val H: Value = Value("H")
  val HR: Value = Value("HR")
  val HOUR: Value = Value("HOUR")
  val HOURS: Value = Value("HOURS")

  val D: Value = Value("D")
  val DAY: Value = Value("DAY")
  val DAYS: Value = Value("DAYS")

  class RichTimeUnit(val unit: String) extends AnyVal {

    def toTimeUnit: TimeUnit = withName(unit.toUpperCase)

    def inMillis: Long = toTimeUnit match {
      case S | SEC | SECOND | SECONDS => 1.second
      case M | MIN | MINUTE | MINUTES => 1.minute
      case H | HR | HOUR | HOURS      => 1.hour
      case D | DAY | DAYS             => 1.day
    }
  }

  class RichTime(val time: Long) extends AnyVal {

    def startOfDay(zone: ZoneId): Long =
      toTimeZone(zone)
        .truncatedTo(ChronoUnit.DAYS)
        .toEpochSecond * 1000

    def startOfNextDay(zone: ZoneId): Long =
      toTimeZone(zone)
        .truncatedTo(ChronoUnit.DAYS)
        .plusDays(1)
        .toEpochSecond * 1000

    def endOfDay(zone: ZoneId): Long = time.startOfNextDay(zone) - 1

    def shiftToZone(zone: ZoneId): Long = toTimeZone(zone).toEpochSecond * 1000

    def asString(
      zone: Option[ZoneId] = None,
      pattern: String = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    ): String =
      toTimeZone(zone.getOrElse(ZoneId.of("UTC")))
        .format(DateTimeFormatter.ofPattern(pattern))

    // Duration methods
    def days: Long = time * 24.hours
    def hours: Long = time * 60.minutes
    def minutes: Long = time * 60.seconds
    def seconds: Long = time * 1000L

    // Same as above but mainly for grammar (e.g. 1.day instead of 1.days)
    def day: Long = days
    def hour: Long = hours
    def minute: Long = minutes
    def second: Long = seconds

    // Round duration to lower resolution
    def roundToSeconds: Long = (time / 1000.0).round
    def roundToMinutes: Long = (time.roundToSeconds / 60.0).round
    def roundToHours: Long = (time.roundToMinutes / 60.0).round
    def roundToDays: Long = (time.roundToHours / 24.0).round

    // Truncate duration to lower resolution
    def truncateToSeconds: Long = time / 1000
    def truncateToMinutes: Long = time.truncateToSeconds / 60
    def truncateToHours: Long = time.truncateToMinutes / 60
    def truncateToDays: Long = time.truncateToHours / 24

    // Get specific temporal field from time
    def hourOfDay(zone: ZoneId): Int = toTimeZone(zone).getHour
    def minuteOfHour(zone: ZoneId): Int = toTimeZone(zone).getMinute
    def dayOfMonth(zone: ZoneId): Int = toTimeZone(zone).getDayOfMonth
    def monthOfYear(zone: ZoneId): Int = toTimeZone(zone).getMonthValue

    private def toTimeZone(zone: ZoneId): ZonedDateTime =
      Instant.ofEpochMilli(time).atZone(zone)
  }

  class RichStringTime(val time: String) extends AnyVal {

    /** Converts time in given pattern to millis; e.g: "2020-01-01 00:00:00" -> 1578586083268
      * @param zone zoneId
      * @param pattern pattern of the input string
      * @return milliseconds as Option[Long]
      */
    def toMillis(zone: ZoneId, pattern: String = "yyyy-MM-dd['T'][ ]HH:mm:ss['Z']"): Option[Long] =
      Try(
        Instant.from(DateTimeFormatter.ofPattern(pattern).withZone(zone).parse(time)).toEpochMilli
      ) match {
        case Failure(e) =>
          println(s"Error parsing \'$time\' using pattern \'$pattern\'", e.getMessage)
          None
        case Success(v) => Some(v)
      }

    /** converts time in format HH:mm:ss to millis, e.g "01:00:01" -> 3601000
      * @return milliseconds as Option[Long]
      */
    def toMillis: Option[Long] =
      Option(time).map(_.split(":").map(_.toLong)).collect {
        case parts: Array[Long] if parts.lengthEquals(3) =>
          parts.reduce(_ * 60L + _) * 1000L
      }
  }
}
