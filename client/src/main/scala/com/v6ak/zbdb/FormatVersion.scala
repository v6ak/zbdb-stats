package com.v6ak.zbdb

import scala.collection.immutable.IndexedSeq

final case class FormatVersion private (versionNumber: Int, hasAgeCategory: Boolean, tail: Tail)

abstract sealed class Tail{
  def split(data: IndexedSeq[IndexedSeq[String]]): (IndexedSeq[IndexedSeq[String]], IndexedSeq[IndexedSeq[String]])
}
object Tail{
  final case class Constant(length: Int) extends Tail{
    override def split(data: IndexedSeq[IndexedSeq[String]]): (IndexedSeq[IndexedSeq[String]], IndexedSeq[IndexedSeq[String]]) = (
      data.dropRight(length),
      data.takeRight(length)
    )
  }
  object EmptyLine extends Tail {
    override def split(data: IndexedSeq[IndexedSeq[String]]): (IndexedSeq[IndexedSeq[String]], IndexedSeq[IndexedSeq[String]]) = {
      val (base, tailWithEmptyLine) = data.span(line => !line.forall(_ == ""))
      (base, tailWithEmptyLine.drop(1))
    }
  }
}

object FormatVersion{

  val Versions: Map[Int, FormatVersion] = Seq(
    FormatVersion(2015, hasAgeCategory = false, tail = Tail.Constant(2)),
    FormatVersion(2016, hasAgeCategory = true, tail = Tail.EmptyLine)
  ).map(fv => fv.versionNumber -> fv).toMap

}