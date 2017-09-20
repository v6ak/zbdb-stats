package com.v6ak.scalajs.scalatags

import org.scalajs.dom.raw.Element

import scalatags.JsDom.all.Modifier

object ScalaTagsDomModifiers {
  def addClass(className: String): Modifier = new Modifier{
    override def applyTo(el: Element): Unit = el.classList.add(className)
  }

}
