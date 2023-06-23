package com.rzk

import com.comcast.ip4s.{Host, Port}
import com.rzk.utils.SinkStatus
import com.rzk.utils.SinkStatus.SinkStatusOps
import net.sigusr.mqtt.api.PredefinedRetryPolicy
import pureconfig._
import pureconfig.error.CannotConvert

import scala.concurrent.duration.{Duration, FiniteDuration}

package object runconfig {

  private[runconfig] val source: ConfigObjectSource = ConfigSource
    .resources("local.conf")
    .optional
    .withFallback(ConfigSource.resources("default.conf"))

  implicit val hostReader: ConfigReader[Host] = ConfigReader[String].emap { hostStr =>
    Host.fromString(hostStr) match {
      case Some(value) => Right(value)
      case None        => Left(CannotConvert(hostStr, "Host", s"$hostStr is not a valid hostname"))
    }
  }

  implicit val portReader: ConfigReader[Port] = ConfigReader[Int].emap { portNum =>
    Port.fromInt(portNum) match {
      case Some(value) => Right(value)
      case None        => Left(CannotConvert(s"$portNum", "Port", s"$portNum is not a valid port number"))
    }
  }

  implicit val retryPolicyReader: ConfigReader[PredefinedRetryPolicy] = ConfigReader[String].emap { policy =>
    policy.toUpperCase match {
      case "CONSTANT_DELAY"      => Right(PredefinedRetryPolicy.ConstantDelay)
      case "EXPONENTIAL_BACKOFF" => Right(PredefinedRetryPolicy.ExponentialBackoff)
      case "FIBONACCI_BACKOFF"   => Right(PredefinedRetryPolicy.FibonacciBackoff)
      case "FULL_JITTER"         => Right(PredefinedRetryPolicy.FullJitter)
      case _ =>
        Left(
          CannotConvert(
            policy,
            "PredefinedRetryPolicy",
            "Only available options are: 'CONSTANT_DELAY', 'EXPONENTIAL_BACKOFF', 'FIBONACCI_BACKOFF', and 'FULL_JITTER'"
          )
        )
    }
  }

  implicit val sinkStatusReader: ConfigReader[SinkStatus] = ConfigReader[String].emap { status =>
    status.toSinkStatus match {
      case Some(value) => Right(value)
      case None =>
        Left(
          CannotConvert(
            status,
            "SinkStatus",
            "Only available options are: 'ACTIVE', 'DEBUG', 'TESTING', and 'DISABLED'"
          )
        )
    }
  }

  implicit val finiteDurationReader: ConfigReader[FiniteDuration] = ConfigReader[Duration].emap { d =>
    Either.cond(
      d.isFinite,
      FiniteDuration(d.length, d.unit),
      CannotConvert(s"$d", "FiniteDuration", s"$d is not a finite duration")
    )
  }
}
