package com.v6ak.zbdb

import scalatags.JsDom.all._

final case class Glyph(frag: Frag) extends AnyVal {
  @inline def toHtml: Frag = frag

}

object Glyphs {
  private def css(name: String) = Glyph(span(`class`:=s"icon-$name"))
  val Pedestrian = Glyph("\uD83D\uDEB6")
  val Globe = Glyph("\uD83C\uDF10")
  val Play = css("play")
  val Pause = css("pause")
  val Timeline = css("timeline")

}
