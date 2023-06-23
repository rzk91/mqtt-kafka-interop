package com.rzk.utils.implicits

import fs2._

class RichOption[A](private val maybe: Option[A]) extends AnyVal {
  def debug: String = maybe.fold("N/A")(_.toString)
  def debug(formatter: A => String, ifNone: => String): String = maybe.fold(ifNone)(formatter)

  // FS2 utils
  def toChunk: Chunk[A] = Chunk.iterator(maybe.iterator)
  def toFs2Stream[F[_]]: Stream[F, A] = Stream.chunk(toChunk)
}
