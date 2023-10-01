package com.v6ak.zbdb

import scala.language.implicitConversions


object EitherPartitioningRichSeq {
  @inline implicit def toPartitioningRichSeq[T, U](seq: Seq[Either[T, U]]): EitherPartitioningRichSeq[T, U] =
    new EitherPartitioningRichSeq(seq)
}

final class EitherPartitioningRichSeq[T, U](val seq: Seq[Either[T, U]]) extends AnyVal {
  def partitionLeftRight: (Seq[T], Seq[U]) = {
    val (leftEithers, rightEithers) = seq.partition(_.isLeft)
    val lefts = leftEithers.map {
      case Left(x) => x
      case value@Right(_) => throw new AssertionError(s"Unexpected $value")
    }
    val rights = rightEithers.map {
      case Right(x) => x
      case value@Left(_) => throw new AssertionError(s"Unexpected $value")
    }
    (lefts, rights)
  }
}
