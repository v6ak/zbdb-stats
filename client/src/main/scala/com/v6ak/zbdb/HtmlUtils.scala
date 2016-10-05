package com.v6ak.zbdb

import org.scalajs.dom

import scala.scalajs.js
import scalatags.JsDom.all._

object HtmlUtils {

  val EmptyHtml: Frag = ""

  def dropdownGroup(mods: Modifier*)(buttons: Frag*) = div(cls:="btn-group")(
    button(cls:="btn btn-normal dropdown-toggle", "data-toggle".attr := "dropdown", "aria-haspopup".attr := "true", "aria-expanded".attr := "false")(mods: _*),
    div(cls:="dropdown-menu")(buttons : _*)
  )

  def modal(title: Frag) = {
    val modalBodyId = IdGenerator.newId()
    val modalHeader = div(`class`:="modal-header")(button(`type`:="button", `class`:="close", "data-dismiss".attr := "modal")(span("aria-hidden".attr := "true")("×")))(h4(`class`:="modal-title")(title))
    val modalBody = div(`class`:="modal-body", id := modalBodyId)
    val modalFooter = div(`class`:="modal-footer")
    val modalDialog = div(`class`:="modal-dialog modal-xxl")(div(`class`:="modal-content")(modalHeader, modalBody, modalFooter))
    val dialog = div(`class`:="modal fade")(modalDialog).render
    val jqModal = dom.window.asInstanceOf[js.Dynamic].$(dialog)
    jqModal.on("hidden.bs.modal", {() => dialog.parentNode.removeChild(dialog)})
    (dialog, jqModal, modalBodyId)
  }

}
