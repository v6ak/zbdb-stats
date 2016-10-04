package com.v6ak.zbdb

import com.example.moment._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object App extends JSApp {

  @JSExport
  override def main(): Unit = {
    dom.window.onload = { _: Any =>
      val fileName = dom.window.document.body.getAttribute("data-file")
      val maxHourDelta = dom.window.document.body.getAttribute("data-max-hour-delta").toInt
      val formatVersionNumber = dom.window.document.body.getAttribute("data-format-version").toInt
      val startTime = moment.tz(dom.window.document.body.getAttribute("data-start-time"), dom.window.document.body.getAttribute("data-timezone"))
      val endTime = moment.tz(dom.window.document.body.getAttribute("data-end-time"), dom.window.document.body.getAttribute("data-timezone"))
      if(!startTime.isValid()) sys.error("startTime is invalid")
      if(!endTime.isValid()) sys.error("endTime is invalid")
      dom.console.log("startTime", startTime.toString)
      dom.console.log("endTime", endTime.toString)
      dom.console.log("fileName", fileName)
      val params = dom.window.location.search.substring(1).split("&").map{paramString =>
        paramString.split("=", 2) match {
          case Array(name, value) => (name, value)
          case Array(name) => (name, "")
        }
      }.toMap
      Ajax.get(fileName, responseType = "text") onComplete {
        case Failure(e) =>
          dom.window.alert("Failed to load data")
        case Success(xhr) =>
          try{
            val formatVersion = FormatVersion.Versions(formatVersionNumber)
            val result @ (parts, data, errors) = Parser.parse(xhr.responseText, startTime, endTime, maxHourDelta, formatVersion)
            val content = dom.document.getElementById("content")
            val participantTable = ParticipantTable(startTime, parts, data, formatVersion)
            Renderer.initialize(participantTable, errors, content, params.contains("horizontalStickyness"))
            Option(dom.document.getElementById("loading-indicator")).foreach{loadingIndicator =>
              loadingIndicator.parentNode.removeChild(loadingIndicator)
            }
          } catch {
            case e: Throwable =>
              dom.console.error("Error when parsing: ", e.getMessage)
              e.printStackTrace()
              dom.window.alert("Nepodařilo se zpracovat data")
          }
      }
    }
  }
}
