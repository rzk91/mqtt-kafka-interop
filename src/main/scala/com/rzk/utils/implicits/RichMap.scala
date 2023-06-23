package com.rzk.utils.implicits

import scala.util.Try

class RichMap[K, V](private val map: Map[K, V]) extends AnyVal {
  def getValueAs[W](key: => K): Option[W] = Try(map(key).asInstanceOf[W]).toOption
  def getValueAsString(key: => K): Option[String] = Try(map(key).toString.trim).toOption
  def getValueAsInt(key: => K): Option[Int] = getValueAsString(key).flatMap(_.asInt)
  def getValueAsLong(key: => K): Option[Long] = getValueAsString(key).flatMap(_.asLong)
  def getValueAsDouble(key: => K): Option[Double] = getValueAsString(key).flatMap(_.asDouble)
  def getValueAsBoolean(key: => K): Option[Boolean] = getValueAsString(key).flatMap(_.asBoolean)

  // Other utility methods
  def getWith[W](key: K)(f: V => W): Option[W] = map.get(key).map(f)
  def getOrElseWith[W](key: K, default: => W)(f: V => W): W = map.get(key).fold(default)(f)

  // Just a serializable version of some non-serializable `Map` methods
  def filterByValues(p: V => Boolean): Map[K, V] = map.filter { case (_, v) => p(v) }
  def filterByKeys(p: K => Boolean): Map[K, V] = map.filter { case (k, _) => p(k) }
  def transformValues[W](f: V => W): Map[K, W] = map.map { case (k, v) => k -> f(v) }
}
