package com.v6ak.zbdb

import com.example.RichMoment.toRichMoment
import com.example.moment._
import com.v6ak.scalajs.tables.{Column, TableHeadCell, TableRenderer}
import com.v6ak.scalajs.time.TimeInterval
import com.v6ak.zbdb.CollectionUtils._
import com.v6ak.zbdb.HtmlUtils._
import com.v6ak.zbdb.PartTimeInfo.Finished
import com.v6ak.zbdb.TextUtils._
import org.scalajs.dom
import org.scalajs.dom.Node
import org.scalajs.dom.raw._

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scalatags.JsDom.all.{i => iTag, name => _, _}

object Renderer{
  private val FirstBadge = div(cls := "label label-success")("1.")

  def initialize(participantTable: ParticipantTable, errors: Seq[(Seq[String], Throwable)], content: Node, plots: Seq[(String, String)], enableHorizontalStickyness: Boolean) = {
    val r = new Renderer(participantTable, errors, content, plots, enableHorizontalStickyness)
    r.initialize()
    r
  }
}

final class Renderer private(participantTable: ParticipantTable, errors: Seq[(Seq[String], Throwable)], content: Node, additionalPlots: Seq[(String, String)], enableHorizontalStickyness: Boolean) {

  private val plotRenderer = new PlotRenderer(participantTable)

  import Renderer._
  import participantTable._
  import plotRenderer._

  private final implicit class RichParticipant(row: Participant){
    def checkboxId = s"part-${row.id}-checkbox"
    def trId = s"part-${row.id}-tr"
  }

  implicit private class RichTime(moment: Moment){
    def timeOnlyDiv = div(title:=moment.toString)(f"${moment.hours()}:${moment.minutes()}%02d")
  }

  private object selection {
    private val selectedParticipants = mutable.Set[Participant]()
    private val selectedNames = selectedParticipants.view.map(r => s"${r.fullName} (${r.id})")
    private val selectedClass = "info"
    def apply() = selectedParticipants.toSet
    def addRow(row: Participant) = {
      dom.document.getElementById(row.trId).classList.add(selectedClass)
      dom.document.getElementById(row.checkboxId).asInstanceOf[HTMLInputElement].checked = true
      selectedParticipants += row
      rowsChanged()
    }
    def removeRow(row: Participant) = {
      dom.document.getElementById(row.trId).classList.remove(selectedClass)
      dom.document.getElementById(row.checkboxId).asInstanceOf[HTMLInputElement].checked = false
      selectedParticipants -= row
      rowsChanged()
    }
    def clear(): Unit ={
      while(selectedParticipants.nonEmpty){
        removeRow(selectedParticipants.head)
      }
    }
    private def rowsChanged() {
      showBar = selectedParticipants.nonEmpty
      while(selectedParticipantsElement.firstChild != null){
        selectedParticipantsElement.removeChild(selectedParticipantsElement.firstChild)
      }
      if(selectedParticipants.nonEmpty){
        selectedParticipantsElement.appendChild((s"K porovnání (${selectedParticipants.size}): "+selectedNames.toSeq.sorted.mkString(", ")).render)
      }
    }
  }

  private val renderer = new TableRenderer[Participant](
    headRows = 2,
    tableModifiers = Seq(`class` := "table table-condensed table-hover"),
    trWrapper = {(tableRow, row) => tableRow(id := row.trId)}
  )(Seq[Column[Participant]](
    Column(TableHeadCell("id, jméno", rowCount = 2))(renderParticipantColumn)(className = "participant-header"),
    Column[Participant]("Kat.")(p => Seq[Frag](Genders(p.gender), br, p.age))
  ) ++ parts.zipWithIndex.flatMap{case (part, i) =>
    createTrackPartColumns(part, i)
  } ++ Seq[Column[Participant]](
    Column(
      TableHeadCell.Empty,
      TableHeadCell(span(title := "Celkový čas")("Celk."))
    ){(p: Participant) =>
      if(p.hasFinished) p.totalTime.toString else ""
    }(),
    Column(
      "Čistý čas chůze"
    ){(p: Participant) =>
      TimeInterval.fromMilliseconds(p.partTimes.flatMap(_.durationOption).sum).toString
    }
  ))

  def createTrackPartColumns(part: Part, i: Int): Seq[Column[Participant]] = {
    def partData(row: Participant) = row.partTimes.lift(i)
    val best = if (firsts.length > i)
      firsts(i)
    else {
      dom.console.warn(s"It seems that nobody has reached part #$i")
      BestParticipantData.Empty
    }
    val firstCell = if (i == 0) TableHeadCell("Start") else TableHeadCell.Empty
    Seq[Column[Participant]](
      Column(firstCell, TableHeadCell("|=>"))((r: Participant) =>
        partData(r).fold[Frag]("–")(pti => Seq(
          pti.startTime.timeOnlyDiv,
          if (best.hasBestStartTime(pti)) FirstBadge else EmptyHtml
        ))
      )(className = "col-start"),
      Column(
        TableHeadCell(span(`class` := "track-length", formatLength(part.trackLength))),
        TableHeadCell(s"čas")
      )((r: Participant) =>
        partData(r).collect { case f: Finished => f }.fold[Frag]("–")(pti => Seq[Frag](
          div(pti.intervalTime.toString)(title := best.durationOption.fold("") { bestDurationMillis =>
            val bestDuration = TimeInterval.fromMilliseconds(bestDurationMillis)
            "Nejlepší: " + bestDuration + "\n" + "Ztráta na nejlepšího:" + (pti.intervalTime - bestDuration)
          }),
          if (best.hasBestDuration(pti)) FirstBadge else EmptyHtml
        ))
      )(className = "col-time"),
      Column(
        TableHeadCell(Seq[Frag](part.place, br, span(`class` := "track-length", formatLength(part.cumulativeTrackLength))), colCount = 2),
        TableHeadCell(span(title := s"Čas příchodu na stanoviště č. ${i + 1} – ${part.place}", "=>|"))
      ) { (r: Participant) =>
        partData(r).collect { case f: Finished => f }.fold("–": Frag) { pti => Seq(
          pti.endTime.timeOnlyDiv,
          if (best.hasBestEndTime(pti)) FirstBadge else EmptyHtml
        )
        }
      }(className = "col-end")
    )
  }

  def renderParticipantColumn(r: Participant): Frag = Seq(
    label(`for` := r.checkboxId, cls := "participant-header-label")(
      r.orderOption.fold(span(cls := "label label-danger label-result")("DNF"))(order => span(cls := "label label-success label-result")(s"$order.")),
      input(`type` := "checkbox", `class` := "participant-checkbox hidden-print", id := r.checkboxId, onchange := { e: Event =>
        val el = e.currentTarget.asInstanceOf[HTMLInputElement]
        el.checked match {
          case true => selection.addRow(r)
          case false => selection.removeRow(r)
        }
      }),
      " ",
      r.id + ": " + r.fullNameWithNick
    ),
    div(`class` := "actions hidden-print")(chartButtons(r))
  )

  private val tableElement = renderer.renderTable(data)

  private val selectedParticipantsElement = span(`class` := "selected-participants")("–").render

  private val barElement = div(id := "button-bar")(
    button("×", `class` := "close")(onclick := {e: Event => selection.clear() }),
    dropdownGroup("Porovnat vybrané účastníky…", cls:="btn btn-primary dropdown-toggle")(
      for(plot <- Plots) yield chartButton(s"Porovnat ${plot.nameAccusative}", selection().toSeq, plot.generator, s"Porovnat ${plot.nameAccusative}")
    )(cls := "btn-group dropup"),
    selectedParticipantsElement
  ).render

  private val globalStats = div(id := "global-stats")(
    button("Porovnání startu a času")(cls := "btn btn-default hidden-print")(onclick := {e: Event =>
      val (dialog, jqModal, modalBodyId) = modal("Porovnání startu a času")
      jqModal.on("shown.bs.modal", {() => globalStatsPlot(modalBodyId, data.filter(p => p.hasFinished))})
      dom.document.body.appendChild(dialog)
      jqModal.modal(js.Dictionary("keyboard" -> true))

    }),
    additionalPlots.map{case (name, url) =>
      a(href:=url, cls:="btn btn-default", target := "_blank")(name)
    }
  ).render

  private def showBar(implicit x: Nothing): Nothing = ???

  private def showBar_=(shown: Boolean): Unit ={
    barElement.style.visibility = if(shown) "" else "hidden"
  }

  private def chartButton(title: String, rowsLoader: => Seq[Participant], plotDataGenerator: Seq[Participant] => PlotData, description: Frag) = button(`class` := "btn btn-default btn-xs")(description)(onclick := {_:Any =>
    val (dialog, jqModal, modalBodyId) = modal(title)
    jqModal.on("shown.bs.modal", {() => initializePlot(modalBodyId, plotDataGenerator(rowsLoader))})
    dom.document.body.appendChild(dialog)
    jqModal.modal(js.Dictionary("keyboard" -> true))
  })

  private def chartButtons(row: Participant) = Seq[Frag](
    for{
      plot <- Plots
      name = s"Graf ${plot.nameGenitive} pro ${row.fullNameWithNick}"
    } yield chartButton(
      name,
      Seq(row),
      plot.generator,
      Seq(span(`class`:=s"glyphicon glyphicon-${plot.glyphiconName}", "aria-hidden".attr := "true"))
    )(title := name),
    dropdownGroup(cls:="btn btn-xs btn-normal dropdown-toggle", "…")(
      chartButton(
        s"Graf pro ${row.fullNameWithNick} a všechny, které potkal(a) na cestě",
        Seq(row) ++ getMates(row),
        generateWalkPlotData,
        Seq[Frag](
          span(`class`:="glyphicon glyphicon-user", "aria-hidden".attr := "true"),
          span(`class`:="glyphicon glyphicon-user", style:="margin-left: -5px;", "aria-hidden".attr := "true"),
          s"Graf pro ${row.fullNameWithNick} a všechny, které potkal(a) na cestě"
        )
      )
    )
  )

  private def showCells(cells: Seq[String]): Frag = addSeparators[Frag](", ")(cells.map(c => code(c)))

  private def showThrowable(rootThrowable: Throwable) = ul(causeStream(rootThrowable).map {
    case CellsParsingException(cells, _) => li("K chybě došlo při zpracování následujících buňek: ", showCells(cells))
    case BadTimeInfoFormatException() => li("Očekáváné varianty: a) nevyplněno, b) pouze čas startu, c) všechny tři časy (start, doba, cíl). Pokud je některý čas nevyplněn, očekává se prázdné políčko nebo \"X\".")
    case MaxHourDeltaExceededException(maxHourDelta, prevTime, currentTime) => li(f"Od ${prevTime.hoursAndMinutes} do ${currentTime.hoursAndMinutes} to trvá více než $maxHourDelta hodin, což je nějaké divné, asi to bude chyba.")
    case e: DeadlineExceededException => li("Tento účastník došel až po konci pochodu.")
    case e => li(iTag(e.getClass.getName), ": ", e.getMessage)
  })

  private def initialize(): Unit = {
    showBar = false
    if(errors.nonEmpty){
      content.appendChild(
        div(cls:="alert alert-danger")(
          s"Některé řádky (celkem ${errors.size}) se nepodařilo zpracovat",
          ul(
            errors.map{case (row, e) =>
              li(
                div(showCells(row)),
                div(showThrowable(e))
              )
            }
          )
        ).render
      )
    }
    content.appendChild(globalStats)
    content.appendChild(tableElement)
    content.appendChild(barElement)
    dom.window.asInstanceOf[js.Dynamic].$(tableElement).stickyTableHeaders(Dictionary("cacheHeaderHeight" -> true, "fixedOffset" -> 0))
    if (enableHorizontalStickyness) {
      HorizontalStickiness.addHorizontalStickiness(tableElement)
    }
  }

}
