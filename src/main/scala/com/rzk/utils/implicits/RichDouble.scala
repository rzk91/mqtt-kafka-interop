package com.rzk.utils.implicits

/** Implicitly available extension methods for a `Double` value
  *
  * Provides rounding, truncating, and range-based methods
  */
class RichDouble(val x: Double) extends AnyVal {

  def isDefined: Boolean = !x.isNaN && !x.isInfinity
  def isUndefined: Boolean = !isDefined

  /** Round up the double value to given number of decimal places
    */
  def roundTo(n: Int): Double = {
    val m = math.pow(10, n)
    (x * m).round / m
  }

  /** Truncate the double value to given number of decimal places
    */
  def truncateTo(n: Int): Double = {
    val m = math.pow(10, n)
    (x * m).floor / m
  }
  def roundTo1: Double = roundTo(1)
  def truncateTo1: Double = truncateTo(1)
  def roundTo2: Double = roundTo(2)
  def truncateTo2: Double = truncateTo(2)

  /** Check if the double value is between min and max (both inclusive by default)
    */
  def inRange(min: Double, max: Double, inclusive: Boolean = true): Boolean = {
    require(max >= min, s"max ($max) cannot be less than min ($min)")
    if (inclusive) x >= min && x <= max else x > min && x < max
  }

  /** This is a variant of the normal `inRange` method for (min-max) tuples
    */
  def inRange(minMax: (Double, Double)): Boolean = inRange(minMax._1, minMax._2)

  /** Perform operation `ifInRange` if double value is in range; else perform `ifOutOfRange`
    */
  def inRangeFold[A](
    min: Double,
    max: Double,
    ifOutOfRange: => A,
    ifInRange: Double => A,
    inclusive: Boolean = true
  ): A =
    if (inRange(min, max, inclusive)) ifInRange(x) else ifOutOfRange

  /** Range sgn function
    *
    *  - Value smaller than `min` are mapped to -1;
    *  - Value greater than `max` are mapped to +1;
    *  - Value in range are mapped to 0
    */
  def rsignum(min: Double, max: Double): Int = {
    require(max >= min, s"max ($max) cannot be less than min ($min)")
    if (x < min) -1 else if (x > max) 1 else 0
  }

  /** This is a variant of the normal `rsignum` method for (min-max) tuples
    */
  def rsignum(minMax: (Double, Double)): Int = rsignum(minMax._1, minMax._2)
}
