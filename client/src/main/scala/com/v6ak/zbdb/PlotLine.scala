package com.v6ak.zbdb

import scala.scalajs.js
import scala.scalajs.js.Dictionary

final private case class PlotLine(row: Participant, label: String, points: js.Array[js.Array[_]]) {
  def seriesOptions = js.Dictionary(
    "label" -> label,
    "highlighter" -> Dictionary(
      "formatString" -> (row.id+": %s|%s|%s|%s")
    )
  )
}
