package com.v6ak.zbdb

import scalatags.JsDom.all._

object Bootstrap {
  val toggle = data.toggle
  val dismiss = data.dismiss
  def glyphicon(name: String) = span(`class`:=s"glyphicon glyphicon-${name}", aria.hidden := "true")
  def btn = button(`class` := "btn")
  def btnDefault = btn(`class` := "btn-default")
}
