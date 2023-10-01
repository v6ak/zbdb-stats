package com.v6ak.zbdb

object CollectionUtils {

  def addSeparators[T](separator: T)(s: Seq[T]): Seq[T] = (Stream.continually(separator) lazyZip s).flatMap{(x, y) => Seq(x, y)}.drop(1)

  def causeStream(e: Throwable): Stream[Throwable] = Stream.cons(e, Option(e.getCause).fold(Stream.empty[Throwable])(causeStream))

}
