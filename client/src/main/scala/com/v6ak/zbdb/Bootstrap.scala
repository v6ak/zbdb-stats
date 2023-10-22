package com.v6ak.zbdb

import org.scalajs.dom._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scalatags.JsDom.all._

import scala.scalajs.js.annotation.JSImport.Namespace

@js.native
@JSImport("bootstrap/js/dist/modal", Namespace)
class BsModal (el: Element) extends js.Any {
  def show(): Unit = js.native
}


object Bootstrap {
  val toggle = attr("data-bs-toggle")
  val dismiss = attr("data-bs-dismiss")
  def btn = button(`class` := "btn")
  def btnPrimary = btn(`class` := "btn-primary")
  def btnDefault = btn(`class` := "btn-secondary")
  def btnLight = btn(`class` := "btn-light")
  implicit final class DialogUtils(val el: Element) extends AnyVal {
    @inline def onBsModalShown(f: js.Function) =
      el.addEventListener("shown.bs.modal", f.asInstanceOf[js.Function1[_, Any]])

    @inline def onBsModalHidden(f: js.Function) =
      el.addEventListener("hidden.bs.modal", f.asInstanceOf[js.Function1[_, Any]])
  }
}
