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
import org.scalajs.dom._

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scalatags.JsDom.all.{i => iTag, name => _, _}
import com.v6ak.scalajs.scalatags.ScalaTagsDomModifiers._
import com.v6ak.scalajs.scalatags.ScalaTagsBootstrapDomModifiers._

object Renderer{
  def renderPositionBadge(position: Option[Int]) =
    position.fold(span(cls := "badge bg-danger badge-result")("DNF"))(order =>
      span(cls := "badge bg-success badge-result")(s"$order.")
    )

  private val FirstBadge = div(cls := "badge bg-success first-badge")("1.")

  def initialize(participantTable: ParticipantTable, processingErrors: Seq[(Seq[String], Throwable)], content: Node, plots: Seq[(String, String)], enableHorizontalStickyness: Boolean, year: String, yearLinksOption: Option[Seq[(String, String)]]) = {
    val r = new Renderer(participantTable, processingErrors, content, plots, enableHorizontalStickyness, year, yearLinksOption)
    r.initialize()
    r
  }
}

final class Renderer private(participantTable: ParticipantTable, processingErrors: Seq[(Seq[String], Throwable)], content: Node, additionalPlots: Seq[(String, String)], enableHorizontalStickyness: Boolean, year: String, yearLinksOption: Option[Seq[(String, String)]]) {

  private val plotRenderer = new PlotRenderer(participantTable)
  private val timeLineRenderer = new TimeLineRenderer(participantTable, plotRenderer)
  private val switches = new ClassSwitches(Map("details" -> "with-details"))

  import Renderer._
  import Bootstrap._
  import participantTable._
  import plotRenderer._

  private final implicit class RichParticipant(row: Participant){
    def checkboxId = s"part-${row.id}-checkbox"
    def trId = s"part-${row.id}-tr"
  }

  implicit private class RichTime(moment: Moment){
    def timeOnlyDiv: Frag = div(title:=moment.toString)(humanReadable)
    def humanReadable: String = f"${moment.hours()}:${moment.minutes()}%02d"
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
    private def rowsChanged(): Unit = {
      showBar = selectedParticipants.nonEmpty
      while(selectedParticipantsElement.firstChild != null){
        selectedParticipantsElement.removeChild(selectedParticipantsElement.firstChild)
      }
      if(selectedParticipants.nonEmpty){
        selectedParticipantsElement.appendChild((s"K porovnání (${selectedParticipants.size}): "+selectedNames.toSeq.sorted.mkString(", ")).render)
      }
    }
  }

  private def yearSelection = dropdownGroup(Seq[Frag](year, " ", span(cls:="caret")))(
    yearLinksOption match{
      case Some(yearLinks) => yearLinks.reverse.map{case (y, yearLink) =>
        a(`class`:="dropdown-item", href:=yearLink)(y)
      }
      case None => span(cls:="badge bg-danger")("Ročníky nejsou k dispozici.")
    }
  )

  private val renderer = new TableRenderer[Participant](
    headRows = 2,
    tableModifiers = Seq(`class` := "table table-sm table-hover participants-table"),
    trWrapper = {(tableRow, row) => tableRow(id := row.trId)}
  )(Seq[Column[Participant]](
    Column(TableHeadCell(yearSelection), TableHeadCell("id, jméno"))(renderParticipantColumn)(className = "participant-header"),
    Column[Participant](TableHeadCell(""), TableHeadCell("Kat."))(p => Seq[Frag](span(cls:="gender")(Genders(p.gender)), " ", span(cls:="age")(p.age)))(className = "category")
  ) ++ Seq[Option[Column[Participant]]](
    if(formatVersion.ageType == AgeType.BirthYear) Some(Column[Participant]("Roč.")(p => Seq[Frag](p.birthYear.get))) else None
  ).flatten ++ Seq(
    Column.rich(TableHeadCell("", rowCount = 2))( (p: Participant) =>
      fseq(timelineButton(p)(`class` := "btn-xl"))
    )(className = "without-details-only")
  ) ++ parts.zipWithIndex.flatMap{case (part, i) =>
    createTrackPartColumns(part, i)
  } ++ Seq[Column[Participant]](
    Column(
      TableHeadCell("", additionalClass = "without-details-only"),
      TableHeadCell(span(title := "Celkový čas")("Celk."))
    ){(p: Participant) =>
      if(p.hasFinished) p.totalTime.toString else ""
    }(),
    Column(
      "Čas od prvního"
    )((p: Participant) =>
      if(p.hasFinished) participantTable.bestTotalTimeOption.fold[Frag]("–")(fastest => (p.totalTime - fastest).toString)
      else "–"
    ),
    Column(
      "Čistý čas chůze"
    ){(p: Participant) =>
      TimeInterval.fromMilliseconds(p.partTimes.flatMap(_.durationOption).sum).toString
    }
  ))

  def createTrackPartColumns(part: Part, i: Int): Seq[Column[Participant]] = {
    def partData(row: Participant) = row.partTimes.lift(i)
    def finishedPartData(row: Participant): Option[PartTimeInfo.Finished] = partData(row).collect{
      case x: PartTimeInfo.Finished => x
    }
    val best = if (firsts.length > i)
      firsts(i)
    else {
      dom.console.warn(s"It seems that nobody has reached part #$i")
      BestParticipantData.Empty
    }
    def moreButton(c: String) = btnDefault(cls := s"btn-sm dropdown-toggle $c", toggle:="dropdown")(span(cls:="caret"))
    val firstCell = if (i == 0) TableHeadCell("Start") else TableHeadCell.Empty
    Seq[Column[Participant]](
      Column.rich(firstCell, TableHeadCell("|=>"))((r: Participant) =>
        partData(r).fold[Seq[Modifier]](Seq("–"))(pti =>
          Seq[Modifier](pti.startTime.timeOnlyDiv) ++ conditionalFirstBadge(best.hasBestStartTime(pti))
        )
      )(className = "col-start detailed-only"),
      Column.rich(
        TableHeadCell(span(`class` := "track-length", formatLength(part.trackLength))),
        TableHeadCell(s"čas")
      )((r: Participant) =>
        partData(r).collect { case f: Finished => f }.fold[Seq[Modifier]](Seq("–"))(pti => {
          val popup = div(cls := "dropdown-menu")("Opravdu malý moment…").render
          val timeDiv = div(pti.intervalTime.toString)(title := best.durationOption.fold("") { bestDurationMillis =>
            val bestDuration = TimeInterval.fromMilliseconds(bestDurationMillis)
            "Nejlepší: " + bestDuration + "\n" + "Ztráta na nejlepšího:" + (pti.intervalTime - bestDuration)
          })
          val detailsExpandable = Seq(
            moreButton("more-button-placeholder"),
            div(cls := "more-dropdown")(
              div(cls := "dropdown")(
                moreButton("more-button"),
                popup
              )(onBsShown({ (_: js.Dynamic) =>
                if (!popup.hasAttribute("data-loaded")) {
                  def select(f: (Finished, Finished) => Boolean) = participantTable.data.filter(p => (p!=r) && finishedPartData(p).exists(f(pti, _)))
                  def compList(heading: Frag, participants: Seq[Participant]): Frag =
                    Seq[Frag](
                      h3(heading),
                      participants match {
                        case Seq() => "(nikdo)": Frag
                        case _ =>
                          ul(
                            participants.map{p =>
                              val pd = partData(p).get
                              li(s"${p.id}: ${p.fullNameWithNick} (${pd.startTime.humanReadable} – ${pd.endTimeOption.fold("×")(_.humanReadable)})")
                            }
                          )
                      }
                    )
                  val details: Frag = Seq(
                    compList("Startoval(a) zároveň s", select((me, other) => me.startTime isSame other.startTime)),
                    compList("Dorazil(a) zároveň s", select((me, other) => me.endTime isSame other.endTime)),
                    compList("Předběhl(a)", select((me, other) => me overtook other)),
                    compList("Byl(a) předběhnuta", select((me, other) => other overtook me))
                  )
                  val detailsRendered = details.render
                  while (popup.hasChildNodes()) {
                    popup.removeChild(popup.lastChild)
                  }
                  popup.appendChild(detailsRendered)
                  popup.setAttribute("data-loaded", "true")
                }
              }))
            )
          )
          Seq(timeDiv) ++ conditionalFirstBadge(best.hasBestDuration(pti)) ++ detailsExpandable
        })
      )(className = "col-time detailed-only"),
      Column.rich(
        TableHeadCell(Seq[Frag](part.place, br, span(`class` := "track-length", formatLength(part.cumulativeTrackLength))), colCount = 2),
        TableHeadCell(span(title := s"Čas příchodu na stanoviště č. ${i + 1} – ${part.place}", "=>|"))
      ) { (r: Participant) =>
        partData(r).collect { case f: Finished => f }.fold[Seq[Modifier]](Seq("–")) { pti =>
          Seq(pti.endTime.timeOnlyDiv) ++ conditionalFirstBadge(best.hasBestEndTime(pti))
        }
      }(className = "col-end detailed-only")
    )
  }

  private def conditionalFirstBadge(first: Boolean) = if (first) Seq[Modifier](FirstBadge, addClass("first")) else Seq()

  def renderParticipantColumn(r: Participant): Frag = Seq(
    label(`for` := r.checkboxId, cls := "participant-header-label")(
      renderPositionBadge(r.orderOption),
      input(`type`:="checkbox", `class`:="participant-checkbox d-print-none", id:=r.checkboxId, onChange { e =>
        val el = e.currentTarget.asInstanceOf[HTMLInputElement]
        el.checked match {
          case true => selection.addRow(r)
          case false => selection.removeRow(r)
        }
      }),
      " ",
      s"${r.id}: ${r.fullNameWithNick}",
    ),
    div(`class` := "actions d-print-none")(chartButtons(r))
  )

  private val tableElement = renderer.renderTable(data)

  private val selectedParticipantsElement = span(`class` := "selected-participants")("–").render

  private val barElement = div(id := "button-bar")(
    button(`class` := "btn-close")(onclick := {(e: Event) => selection.clear() }),
    dropdownGroup("Porovnat vybrané účastníky…", cls:="btn btn-primary dropdown-toggle")(
      for(plot <- Plots) yield chartButton(s"Porovnat ${plot.nameAccusative}", selection().toSeq, plot.generator, s"Porovnat ${plot.nameAccusative}")
    )(cls := "btn-group dropup"),
    selectedParticipantsElement
  ).render

  private val globalStats = div(id := "global-stats")(
    GlobalPlots.map { case (plotName, (detailedTitleOption, plotFunction)) =>
      button(plotName)(cls := "btn btn-secondary d-print-none")(onclick := {(e: Event) =>
        val (dialog, modalBodyId, bsMod) = modal(detailedTitleOption.getOrElse(plotName): String)
        dialog.onBsModalShown{ () => plotFunction(modalBodyId, data, participantTable) }
        dom.document.body.appendChild(dialog)
        bsMod.show()
      })
    },
    additionalPlots.map{case (name, url) =>
      a(href:=url, cls:="btn btn-secondary", target := "_blank")(name)
    }
  ).render

  private def showBar(implicit x: Nothing): Nothing = ???

  private def showBar_=(shown: Boolean): Unit ={
    barElement.style.visibility = if(shown) "" else "hidden"
  }

  private def chartButton(title: String, rowsLoader: => Seq[Participant], plotDataGenerator: Seq[Participant] => PlotData, description: Frag) =
    btnDefault(`class` := "btn-sm")(description)(onclick := {(_:Any) =>
      val (dialog, modalBodyId, bsMod) = modal(title)
      dialog.onBsModalShown({() => initializePlot(modalBodyId, plotDataGenerator(rowsLoader))})
      dom.document.body.appendChild(dialog)
      bsMod.show()
    })

  private def chartButtons(row: Participant) = span(cls := "detailed-only")(
    timelineButton(row)(`class` := "btn-sm")
  )

  private def timelineButton(row: Participant) =
    btnPrimary(
      Glyphs.Timeline.toHtml,
      onclick := { (_: Any) =>
        val (dialog, modalBodyId, bsMod) = modal(s"Časová osa pro #${row.id}: ${row.fullNameWithNick}")
        dom.document.body.appendChild(dialog)
        val (timeLine, renderPlots) = timeLineRenderer.timeLine(row)
        dom.document.getElementById(modalBodyId).appendChild(timeLine)
        timeLineRenderer.bodyClasses.foreach(dom.document.body.classList.add)
        bsMod.show()
        dialog.onBsModalShown(renderPlots)
      }
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
    switches.values.foreach(dom.document.body.classList.add)
    content.appendChild(
      div(`class` := "container")(
        yearLinksOption match {
          case Some(yearLinks) if !yearLinks.map(_._1).contains(year) => fseq(
            div(cls := "alert alert-danger")(
              "Výsledky pro tento ročník ještě nejsou finální.", br,
              "Pro adminy: pokud je to již hotovo, jdi do adresáře statistiky a přepiš soubor years.json souborem " +
                "years.json.new. Tím schválíš všechny nové ročníky jako finální."
            )
          )
          case None => fseq(
            div(cls := "alert alert-danger")(
              "Neporařilo se načíst seznam ročníků. Takže vlastně ani nevím, jestli jsou toto finální výsledky."
            )
          )
          case _ => fseq()
        },
        if (processingErrors.nonEmpty) fseq(
          div(cls := "alert alert-danger")(
            s"Některé řádky (celkem ${processingErrors.size}) se nepodařilo zpracovat",
            ul(
              processingErrors.map { case (row, e) =>
                li(
                  div(showCells(row)),
                  div(showThrowable(e))
                )
              }
            )
          )
        ) else fseq(),
        h2("Grafy"),
        globalStats,
        h2("Výsledky"),
        switches.checkbox("details", "Ukázat stručnou tabulku")(
          "without-details", "with-details"
        ),
      ).render
    )
    content.appendChild(tableElement)
    content.appendChild(barElement)
  }

}
