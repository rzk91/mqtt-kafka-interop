package com.rzk.utils.implicits

import scala.util.Try

class RichTry[A](private val tried: Try[A]) extends AnyVal {
  // Additional methods based on `Option` class
  def exists(f: A => Boolean): Boolean = tried.isSuccess && f(tried.get)

  def forall(f: A => Boolean): Boolean = tried.isFailure || f(tried.get)

  def contains[B >: A](b: B): Boolean = tried.isSuccess && b == tried.get

  def toList: List[A] = tried.toOption.toList

  def toSet: Set[A] = tried.toOption.toSet
}
