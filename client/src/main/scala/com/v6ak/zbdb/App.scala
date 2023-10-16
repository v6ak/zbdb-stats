package com.v6ak.zbdb

import com.example.moment._
import com.example.MomentTimezone.tz
import org.scalajs.dom
import org.scalajs.dom._
import scala.scalajs.js.Thenable.Implicits._

import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.util.{Failure, Success}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object App {

  private def init(): Unit = {
    // Modules with side effects
    Require.require("bootstrap/js/dist/dropdown")
    Require.require("chartjs-adapter-moment")
  }

  @JSExport
  def main(args: Array[String]): Unit = {
    init()
    dom.window.onload = { (_: Any) =>
      val body = dom.window.document.body
      val fileName = body.getAttribute("data-file")
      val maxHourDelta = body.getAttribute("data-max-hour-delta").toInt
      val formatVersionNumber = body.getAttribute("data-format-version").toInt
      val startTime = tz(body.getAttribute("data-start-time"), body.getAttribute("data-timezone"))
      val endTime = tz(body.getAttribute("data-end-time"), body.getAttribute("data-timezone"))
      if(!startTime.isValid()) sys.error("startTime is invalid")
      if(!endTime.isValid()) sys.error("endTime is invalid")
      dom.console.log("startTime", startTime.toString)
      dom.console.log("endTime", endTime.toString)
      dom.console.log("fileName", fileName)
      val params = dom.window.location.search.drop(1).split("&").map{paramString =>
        paramString.split("=", 2) match {
          case Array(name, value) => (name, value)
          case Array(name) => (name, "")
        }
      }.toMap
      val resultsFuture = dom.fetch(fileName).flatMap(_.text())
      val allYearsLinksFuture = dom.fetch("../../statistiky/years.json").flatMap(_.json()).map{ ay =>
        Some(ay.asInstanceOf[js.Dictionary[String]].toIndexedSeq.sorted)
      }.recover{case e =>
        e.printStackTrace()
        None
      }
      resultsFuture onComplete {
        case Failure(e) =>
          dom.window.alert("Failed to load data")
        case Success(csvData) =>
          try{
            val formatVersion = FormatVersion.Versions(formatVersionNumber)
            val result @ (parts, data, errors) = Parser.parse(csvData, startTime, endTime, maxHourDelta, formatVersion)
            val content = dom.document.getElementById("content")
            val participantTable = ParticipantTable(startTime, parts, data, formatVersion)
            val plots = JSON.parse(body.getAttribute("data-plots")).asInstanceOf[js.Array[js.Array[String]]].toIndexedSeq.map{a => (a(0), a(1))}
            val year = body.getAttribute("data-year")
            allYearsLinksFuture onComplete {
              case Success(yearLinksOption) =>
                Renderer.initialize(participantTable, errors, content, plots, params.contains("horizontalStickyness"), year, yearLinksOption)
              case Failure(exception) =>
                exception.printStackTrace()
                dom.window.alert("Jste svědkem/svědkyní chyby, která neměla nastat!")
            }
          } catch {
            case e: Throwable =>
              dom.console.error("Error when parsing: ", e.getMessage)
              e.printStackTrace()
              dom.window.alert("Nepodařilo se zpracovat data")
          } finally {
            Option(dom.document.getElementById("loading-indicator")).foreach{loadingIndicator =>
              loadingIndicator.parentNode.removeChild(loadingIndicator)
            }
          }
      }
    }
  }
}
