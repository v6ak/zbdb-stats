package com.v6ak.zbdb

import scala.scalajs.js
import scala.language.implicitConversions

abstract sealed class Gender(){
  def inflect[T](feminine:T, masculine: T): T
}

object Gender{
  case object Male extends Gender {
    override def inflect[T](feminine: T, masculine: T): T = masculine
  }
  case object Female extends Gender {
    override def inflect[T](feminine: T, masculine: T): T = feminine
  }
}

final class RichGenderSeq(val seq: Seq[Gender]) extends AnyVal {
  def inflectCzech[T](feminine: T, masculine: T): T = if(seq.contains(Gender.Male)) masculine else feminine
  def inflectCzech[T](feminineSingular: T, masculineSingular: T, femininePlural: T, masculinePlural: T): T = seq match {
    case Seq(one) => one.inflect(feminine = feminineSingular, masculine = masculineSingular)
    case _ => inflectCzech(feminine = femininePlural, masculine = masculinePlural)
  }
}

object RichGenderSeq {
  @inline implicit def toRichGenderSeq(seq: Seq[Gender]): RichGenderSeq = new RichGenderSeq(seq)
  @inline implicit def toRichGenderSeq(seq: js.Array[Gender]): RichGenderSeq = new RichGenderSeq(seq.toSeq)

}
