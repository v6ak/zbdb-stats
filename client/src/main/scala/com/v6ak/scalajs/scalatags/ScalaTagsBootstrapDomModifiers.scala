package com.v6ak.scalajs.scalatags

import org.scalajs.dom
import org.scalajs.dom.Element

import scala.scalajs.js
import scalatags.JsDom.all.Modifier

object ScalaTagsBootstrapDomModifiers {

  def onBsShown(handler: js.Function1[js.Dynamic, Unit]): Modifier = new Modifier{
    override def applyTo(el: Element): Unit = dom.window.asInstanceOf[js.Dynamic].`$`(el).on("show.bs.dropdown", handler)
  }

}
