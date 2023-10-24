package com.v6ak.zbdb

import org.scalajs.dom
import org.scalajs.dom.*

import scala.scalajs.js
import scalatags.JsDom.all.*
import Bootstrap.*

import scala.annotation.tailrec

object HtmlUtils:

  val EmptyHtml: Frag = ""

  def dropdownGroup(mods: Modifier*)(buttons: Frag*) = div(cls:="btn-group")(
    btnLight(cls:="dropdown-toggle", toggle := "dropdown", aria.haspopup := "true", aria.expanded := "false")(mods: _*),
    ul(cls:="dropdown-menu")(buttons.map(li(_)) : _*)
  )

  def modal(title: Frag, keyboard: Boolean = true) = {
    val modalHeader = div(`class`:="modal-header")(
      h5(`class`:="modal-title")(title),
      button(`type`:="button", `class`:="btn-close", dismiss := "modal", aria.label := "Zavřít"),
    )
    val modalBody = div(`class`:="modal-body").render
    val modalFooter = div(`class`:="modal-footer")
    val modalDialog = div(`class`:="modal-dialog modal-xxl")(
      div(`class`:="modal-content")(
        modalHeader, modalBody, modalFooter
      )
    )
    val dialog = div(`class`:="modal fade", attr("data-bs-keyboard") := keyboard.toString)(modalDialog).render
    dialog.onBsModalHidden({() => dialog.parentNode.removeChild(dialog)})
    val bsMod = new BsModal(dialog)
    (dialog, modalBody, bsMod)
  }

  @inline def fseq(frags: Frag*): Seq[Frag] = frags

  def spaceSeparated(values: Frag*): Frag = values.map(fseq(_, " "))

  def onChange(e: js.Function1[Event, Unit]) = onchange := e

  @tailrec def findParent(name: String, el: Element): Element =
    if el.nodeName.toLowerCase == name then el
    else findParent(name, el.parentNode.asInstanceOf[Element])

  @tailrec def findParent(names: Set[String], el: Element): Option[Element] =
    if el == null then None
    else
      if names contains el.nodeName.toLowerCase then Some(el)
      else findParent(names, el.parentNode.asInstanceOf[Element])