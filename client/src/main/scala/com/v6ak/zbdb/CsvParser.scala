package com.v6ak.zbdb

import scala.scalajs.js
import scala.scalajs.js.annotation._

object CsvParser {

  @JSImport("comma-separated-values", JSImport.Namespace)
  @js.native
  private val csvParser: js.Dynamic = js.native

  def parse(s: String): IndexedSeq[IndexedSeq[String]] = csvParser.parse(s, js.Dynamic.literal(
    cast = false
  )).asInstanceOf[js.Array[js.Array[String]]].map(_.toIndexedSeq).toIndexedSeq
}
