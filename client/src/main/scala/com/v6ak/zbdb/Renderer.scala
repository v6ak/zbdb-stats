package com.v6ak.zbdb

import com.example.RichMoment.toRichMoment
import com.example.moment.*
import com.v6ak.scalajs.scalatags.ScalaTagsBootstrapDomModifiers.*
import com.v6ak.scalajs.tables.{Column, TableHeadCell, TableRenderer}
import com.v6ak.scalajs.time.TimeInterval
import com.v6ak.zbdb.Bootstrap.*
import com.v6ak.zbdb.ChartJsUtils.*
import com.v6ak.zbdb.CollectionUtils.*
import com.v6ak.zbdb.HtmlUtils.*
import com.v6ak.zbdb.PartTimeInfo.Finished
import com.v6ak.zbdb.Renderer.*
import com.v6ak.zbdb.TextUtils.*
import org.scalajs.dom
import org.scalajs.dom.*
import scalatags.JsDom.all.{i as iTag, html as _, name as _, *}

import scala.collection.mutable
import scala.scalajs.js

object Renderer:
  def renderPositionBadge(position: Option[Int]) =
    position.fold(span(cls := "badge bg-danger badge-result")("DNF"))(order =>
      span(cls := "badge bg-success badge-result")(s"$order.")
    )

  private val FirstBadge = div(cls := "badge bg-success first-badge")("1.")

  def initialize(
    participantTable: ParticipantTable,
    processingErrors: Seq[(Seq[String], Throwable)],
    content: Node,
    plots: Seq[(String, String)],
    year: String,
    yearLinksOption: Option[Seq[(String, String)]],
  ): Renderer =
    val r = new Renderer(participantTable, processingErrors, content, plots, year, yearLinksOption)
    r.initialize()
    r

final class Renderer private(
  participantTable: ParticipantTable,
  processingErrors: Seq[(Seq[String], Throwable)],
  content: Node,
  additionalPlots: Seq[(String, String)],
  year: String,
  yearLinksOption: Option[Seq[(String, String)]],
):

  private val plotRenderer = new PlotRenderer(participantTable)
  private val timeLineRenderer = new TimeLineRenderer(participantTable, plotRenderer)
  private val switches = new ClassSwitches(
    Map("details" -> "without-details"),
    Map("details" -> "details-switched"),
    (name, cls, f) => {
      document.body.style.opacity = "0.5"
      window.setTimeout(
        () => {
          f()
          window.setTimeout(
            () => {
              document.body.style.opacity = "1"
            },
            0
          )
        },
        0
      )
    }
  )

  import participantTable.*
  import plotRenderer.*

  private final implicit class RichParticipant(row: Participant):
    def checkboxId = s"part-${row.id}-checkbox"
    def trId = s"part-${row.id}-tr"

  implicit private class RichTime(moment: Moment):
    def timeOnlyDiv: Frag = div(title:=moment.toString)(humanReadable)
    def humanReadable: String = f"${moment.hours()}:${moment.minutes()}%02d"

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
        selectedParticipantsElement.appendChild(
          (s"K porovnání (${selectedParticipants.size}): "+selectedNames.toSeq.sorted.mkString(", ")).render
        )
      }
    }
  }

  private def yearSelection = dropdownGroup(Seq[Frag](year, " ", span(cls:="caret")))(
    yearLinksOption match
      case Some(yearLinks) => yearLinks.reverse.map{case (y, yearLink) =>
        a(`class`:="dropdown-item", href:=yearLink)(y)
      }
      case None => span(cls:="badge bg-danger")("Ročníky nejsou k dispozici.")
  )

  val detailsValues = Set("with-details", "without-details")
  val expandCollapseStyle = Seq(`class`:="switch")
  val expandButton = switches.button("details", "with-details", detailsValues)(expandCollapseStyle)("")
  val collapseButton = switches.button("details", "without-details", detailsValues)(
    expandCollapseStyle, `class`:="btn btn-secondary"
  )("Zobrazit stručně")

  private val renderer = new TableRenderer[Participant](
    headRows = 2,
    tableModifiers = Seq(`class` := "table table-sm participants-table"),
    trWrapper = {(tableRow, row) => tableRow(id := row.trId)}
  )(Seq[Column[Participant]](
    Column(
      TableHeadCell(fseq(
        collapseButton(`class` := "detailed-only"),
        yearSelection,
      )),
      TableHeadCell("id, jméno")
    )(renderParticipantColumn)(className = "participant-header"),
    Column[Participant](
      TableHeadCell(""),
      TableHeadCell("Kat.")
    )(p => Seq[Frag](span(cls:="gender")(Genders(p.gender)), " ", span(cls:="age")(p.age)))(className = "category")
  ) ++ Seq[Option[Column[Participant]]](
    if(formatVersion.ageType == AgeType.BirthYear) Some(Column[Participant]("Roč.")(p => Seq[Frag](p.birthYear.get))) else None
  ).flatten ++ Seq(
    Column.singleCell[Participant](TableHeadCell(fseq(expandButton), rowCount = 2))(expandButton)(
      className = "without-details-only expand-columns"
    ),
  ) ++ parts.zipWithIndex.flatMap{case (part, i) =>
    createTrackPartColumns(part, i)
  } ++ Seq[Column[Participant]](
    Column(
      TableHeadCell("", additionalClass = "without-details-only dont-highlight"),
      TableHeadCell(span(title := "Celkový čas")("Celk."))
    ){(p: Participant) =>
      if(p.hasFinished) p.totalTime.toString else ""
    }(),
    Column("Čas od prvního")((p: Participant) =>
      if(p.hasFinished) participantTable.bestTotalTimeOption.fold[Frag]("–")(fastest => s"${p.totalTime - fastest}")
      else "–"
    ),
    Column(
      "Čistý čas chůze"
    ){(p: Participant) =>
      TimeInterval.fromMilliseconds(p.partTimes.flatMap(_.durationOption).sum).toString
    },
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
                  def select(f: (Finished, Finished) => Boolean) =
                    participantTable.data.filter(p => (p!=r) && finishedPartData(p).exists(f(pti, _)))
                  def compList(heading: Frag, participants: Seq[Participant]): Frag =
                    Seq[Frag](
                      h3(heading),
                      participants match
                        case Seq() => "(nikdo)": Frag
                        case _ =>
                          ul(
                            participants.map{p =>
                              val pd = partData(p).get
                              li(s"${p.identification} (${pd.startTime.humanReadable} – " +
                                s"${pd.endTimeOption.fold("×")(_.humanReadable)})")
                            }
                          )
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
        TableHeadCell(
          fseq(part.place, br, span(`class` := "track-length", formatLength(part.cumulativeTrackLength))),
          colCount = 2
        ),
        TableHeadCell(span(title := s"Čas příchodu na stanoviště č. ${i + 1} – ${part.place}", "=>|"))
      ) { (r: Participant) =>
        partData(r).collect { case f: Finished => f }.fold[Seq[Modifier]](Seq("–")) { pti =>
          Seq(pti.endTime.timeOnlyDiv) ++ conditionalFirstBadge(best.hasBestEndTime(pti))
        }
      }(className = "col-end detailed-only")
    )
  }

  private def conditionalFirstBadge(first: Boolean) = if (first) Seq[Modifier](FirstBadge, `class`:="first") else Seq()

  def renderParticipantColumn(r: Participant): Frag = Seq(
    label(`for` := r.checkboxId, cls := "participant-header-label")(
      renderPositionBadge(r.orderOption),
      input(`type`:="checkbox", `class`:="participant-checkbox d-print-none", id:=r.checkboxId, onChange { e =>
        val el = e.currentTarget.asInstanceOf[HTMLInputElement]
        if el.checked then selection.addRow(r)
        else selection.removeRow(r)
      }),
      " ",
      s"${r.briefIdentification}",
    ),
    div(`class` := "actions d-print-none")(chartButtons(r))
  )

  private val tableElement = renderer.renderTable(data)

  private val selectedParticipantsElement = span(`class` := "selected-participants")("–").render

  private val barElement = div(id := "button-bar")(
    button(`class` := "btn-close")(onclick := {(e: Event) => selection.clear() }),
    dropdownGroup("Porovnat vybrané účastníky…", cls:="btn btn-primary dropdown-toggle")(
      for(plot <- IndividualPlots) yield
        chartButton(
          s"Porovnat ${plot.nameAccusative}",
          selection().toSeq,
          plot.generator,
          s"Porovnat ${plot.nameAccusative}"
        )
    )(cls := "btn-group dropup"),
    selectedParticipantsElement
  ).render

  private val globalStats = div(id := "global-stats")(
    GlobalPlots.map { case (plotName, (detailedTitleOption, plotFunction)) =>
      button(plotName)(cls := "btn btn-secondary d-print-none")(onclick := {(e: Event) =>
        val (dialog, modalBody, bsMod) = modal(detailedTitleOption.getOrElse(plotName): String)
        dialog.onBsModalShown{ () => plotFunction(modalBody, data, participantTable) }
        dom.document.body.appendChild(dialog)
        bsMod.show()
      })
    },
    additionalPlots.map{case (name, url) =>
      a(href:=url, cls:="btn btn-secondary", target := "_blank")(name)
    }
  ).render

  private def showBar(implicit x: Nothing): Nothing = ??? // needed in order to behave like a property

  private def showBar_=(shown: Boolean): Unit = barElement.style.visibility = if(shown) "" else "hidden"

  private def chartButton(
    title: String,
    rowsLoader: => Seq[Participant],
    plotDataGenerator: Seq[Participant] => PlotData,
    description: Frag
   ) =
    btnDefault(`class` := "btn-sm")(description)(onclick := { (_:Any) =>
      val (dialog, modalBody, bsMod) = modal(title)
      dialog.onBsModalShown({() =>
        initializePlot(modalBody, plotDataGenerator(rowsLoader), cb => dialog.onBsModalHidden(cb))
      })
      dom.document.body.appendChild(dialog)
      bsMod.show()
    })

  private def chartButtons(row: Participant) = timelineButton(row)(`class` := "btn-sm btn-timeline")

  private def timelineButton(row: Participant) =
    btnPrimary(
      "Detaily",
      onclick := { (_: Any) =>
        val (dialog, modalBody, bsMod) = modal(s"Časová osa pro ${row.identification}")
        dom.document.body.appendChild(dialog)
        val (timeLine, renderPlots) = timeLineRenderer.timeLine(row)
        modalBody.appendChild(timeLine)
        timeLineRenderer.bodyClasses.foreach(dom.document.body.classList.add)
        bsMod.show()
        dialog.onBsModalShown(() => renderPlots(dialog))
      }
    )

  private def showCells(cells: Seq[String]): Frag = addSeparators[Frag](", ")(cells.map(c => code(c)))

  private def showThrowable(rootThrowable: Throwable) = ul(causeStream(rootThrowable).map {
    case CellsParsingException(cells, _) => li("K chybě došlo při zpracování následujících buňek: ", showCells(cells))
    case BadTimeInfoFormatException() => li("Očekáváné varianty: a) nevyplněno, b) pouze čas startu, " +
      "c) všechny tři časy (start, doba, cíl). Pokud je některý čas nevyplněn, očekává se prázdné políčko nebo \"X\".")
    case MaxHourDeltaExceededException(maxHourDelta, prevTime, currentTime) => li(f"Od ${prevTime.hoursAndMinutes} do" +
      f" ${currentTime.hoursAndMinutes} to trvá více než $maxHourDelta hodin, což je nějaké divné, asi to bude chyba.")
    case e: DeadlineExceededException => li("Tento účastník došel až po konci pochodu.")
    case e => li(iTag(e.getClass.getName), ": ", e.getMessage)
  })

  private def initialize(): Unit =
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
      ).render
    )
    content.appendChild(tableElement)
    watchMouseOver(tableElement)
    content.appendChild(barElement)
    HorizontalStickiness.addHorizontalStickiness(tableElement)

  val EmptyNodeList = document.createElement("div").getElementsByClassName(".foo")

  def watchMouseOver(table: html.Table): Unit =
    var highlighted: Set[String] = Set()
    table.addEventListener("mouseover", (e: MouseEvent) => {
      findParent(Set("td", "th"), e.target.asInstanceOf[Element]) match
        case None =>
          console.log("no parent cell for", e.target)
        case Some(cell) =>
          val cols = Option(cell.getAttribute("data-cols")).getOrElse("").split(' ').toSet
          val colsToHighlight = cols -- highlighted
          val colsToUnhighlight = highlighted -- cols
          def findCols(set: Set[String]) =
            if set.isEmpty then EmptyNodeList
            else table.querySelectorAll(set.map(s=> s".col-$s").mkString(","))
          findCols(colsToUnhighlight).foreach(_.classList.remove("highlighted"))
          findCols(colsToHighlight).foreach(_.classList.add("highlighted"))
          highlighted = cols
    })