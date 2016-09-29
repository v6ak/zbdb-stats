package com.v6ak.zbdb

final case class FormatVersion private (versionNumber: Int, hasAgeCategory: Boolean, headLength: Int, tailLength: Int)

object FormatVersion{

  val Versions: Map[Int, FormatVersion] = Seq(
    FormatVersion(2015, hasAgeCategory = false, headLength = 0, tailLength = 2),
    FormatVersion(2016, hasAgeCategory = true, headLength = 1, tailLength = 0)
  ).map(fv => fv.versionNumber -> fv).toMap

}