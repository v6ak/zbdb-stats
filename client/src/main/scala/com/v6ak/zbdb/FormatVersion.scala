package com.v6ak.zbdb

import scala.collection.immutable.IndexedSeq

sealed abstract class NameFormat {
  def parse(participantDataAfterNum: Seq[String]): (String, String, String, Seq[String])
  def size: Int
}

object NameFormat {
  object Split extends NameFormat{
    override def size: Int = 3
    override def parse(participantDataAfterNum: Seq[String]): (String, String, String, Seq[String]) = participantDataAfterNum match {
      case Seq(lastName, firstName, nick, other @_*) => (firstName, lastName, nick, other)
    }
  }
  object Single extends NameFormat{
    override def size: Int = 1
    override def parse(participantDataAfterNum: Seq[String]): (String, String, String, Seq[String]) = participantDataAfterNum match {
      case Seq("", other @_*) => ("", "", "", other)
      case Seq(nameWithPotentialNickBloated, other @_*) =>
        val nameWithPotentialNick = nameWithPotentialNickBloated.trim
        val (nick, fullName) = nameWithPotentialNick.indexOf('(') match {
          case -1 => ("", nameWithPotentialNick)
          case leftParenPos if nameWithPotentialNick.last == ')' =>
            (
              nameWithPotentialNick.substring(0, leftParenPos),
              nameWithPotentialNick.substring(leftParenPos+1, nameWithPotentialNick.size - 1)
            )
        }
        fullName.split(" ", 2) match {
          case Array(firstName, lastName) => (firstName.trim, lastName.trim, nick.trim, other)
          case other => sys.error("Name looks like it contains a nick, but it is not in expected format “nick (FirstName LastName)”.")
        }
    }
  }
}

final case class FormatVersion private (versionNumber: Int, ageType: AgeType, tail: Tail, headSize: Int, nameFormat: NameFormat)

abstract sealed class AgeType {}
object AgeType {
  case object BirthYear extends AgeType
  case object No extends AgeType
  case object Category extends AgeType
}

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
    FormatVersion(2015, ageType = AgeType.No, tail = Tail.Constant(2), headSize = 1, nameFormat = NameFormat.Split),
    FormatVersion(2016, ageType = AgeType.Category, tail = Tail.EmptyLine, headSize = 1, nameFormat = NameFormat.Split),
    FormatVersion(2017, ageType = AgeType.BirthYear, tail = Tail.EmptyLine, headSize = 2, nameFormat = NameFormat.Single),
    FormatVersion(2021, ageType = AgeType.Category, tail = Tail.EmptyLine, headSize = 2, nameFormat = NameFormat.Single)
  ).map(fv => fv.versionNumber -> fv).toMap

}