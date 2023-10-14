package com.v6ak.zbdb

object CollectionUtils {
  def addSeparators[T](separator: T)(s: Seq[T]): Seq[T] =
    (LazyList.continually(separator) lazyZip s).flatMap{(x, y) => Seq(x, y)}.drop(1)
  def causeStream(e: Throwable): LazyList[Throwable] =
    LazyList.cons(e, Option(e.getCause).fold(LazyList.empty[Throwable])(causeStream))

  implicit class RichMap[K, V](val map: Map[K, V]) extends AnyVal {
    def mapValuesStrict[W](f: V => W): Map[K, W] = map.view.mapValues(f).toMap
  }
}
