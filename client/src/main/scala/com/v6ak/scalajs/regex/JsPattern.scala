package com.v6ak.scalajs.regex

import scala.scalajs.js
import scala.language.implicitConversions


class JsPattern(val regex: js.RegExp) extends AnyVal {
  def unapplySeq(s: String): Option[Seq[String]] = regex.exec(s) match {
    case null => None
    case parts => Some(parts.toSeq.drop(1).asInstanceOf[Seq[String]])
  }
}

object JsPattern {

  class JsPatternFactory(val s: String) extends AnyVal {
    @inline def jsr: JsPattern = new JsPattern(new js.RegExp(s))
  }

  @inline implicit def wrapString(s: String): JsPatternFactory = new JsPatternFactory(s)

}
