package com.v6ak.zbdb

import com.example.RichMoment.toRichMoment
import com.example.moment._
import com.v6ak.scalajs.tables.{Column, TableHeadCell, TableRenderer}
import com.v6ak.scalajs.time.TimeInterval
import com.v6ak.zbdb.PartTimeInfo.Finished
import com.v6ak.zbdb.`$`.jqplot.DateAxisRenderer
import org.scalajs.dom
import org.scalajs.dom.Node
import org.scalajs.dom.raw._

import scala.annotation.tailrec
import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scalatags.JsDom.all._

object IdGenerator{

  private var i = 0

  def newId() = {
    i += 1
    "generated-id-"+i
  }

}

object Renderer{
  def initialize(participantTable: ParticipantTable, errors: Seq[(Seq[String], Throwable)], content: Node, enableHorizontalStickyness: Boolean) = {
    val r = new Renderer(participantTable, errors, content, enableHorizontalStickyness)
    r.initialize()
    r
  }
}

@js.native object `$` extends js.Object{
  @js.native object jqplot extends js.Object{
    @js.native class LinearAxisRenderer extends js.Object{
      def createTicks(plot: js.Dynamic): Unit = js.native
    }
    @js.native class DateAxisRenderer extends LinearAxisRenderer{
      override def createTicks(plot: js.Dynamic): Unit = js.native
    }
  }
}

final case class ParticipantPlotGenerator(nameGenitive: String, nameAccusative: String, glyphiconName: String, generator: Seq[Participant] => PlotData)

final class Renderer private(participantTable: ParticipantTable, errors: Seq[(Seq[String], Throwable)], content: Node, enableHorizontalStickyness: Boolean) {

  import ParticipantTable._
  import participantTable._

  def zeroMoment = moment("2000-01-01") // We don't want to mutate it

  private val Plots = Seq(
    ParticipantPlotGenerator("chůze", "chůzi", "globe", generateWalkPlotData),
    ParticipantPlotGenerator("rychlosti", "rychlost", "play", generateSpeedPlotData),
    ParticipantPlotGenerator("pauz", "pauzy", "pause", generatePausesPlotData)
  )

  private val DurationRenderer: js.ThisFunction = (th: js.Dynamic) => {
    dom.window.asInstanceOf[js.Dynamic].$.jqplot.DateAxisRenderer.call(th)
    dom.console.log("tickRenderer: ", th.tickRenderer)
  }
  DurationRenderer.asInstanceOf[js.Dynamic].prototype = new DateAxisRenderer()
  DurationRenderer.asInstanceOf[js.Dynamic].prototype.init = ((th: js.Dynamic) => {
    dom.window.asInstanceOf[js.Dynamic].$.jqplot.DateAxisRenderer.prototype.init.call(th)
    th.tickOptions.formatter = ((format: String, value: Moment) => {
      dom.console.log("value", value)
      val diff = value - zeroMoment
      val hours = diff/1000/60/60
      val minutes = diff/1000/60 - hours*60
      f"$hours%02d:$minutes%02d"
    }): js.Function
  }): js.ThisFunction

  private final val BarRenderer = dom.window.asInstanceOf[js.Dynamic].$.jqplot.BarRenderer

  private def expandSeq[T](s: Seq[T], empty: T, size: Int) = s ++ Seq.fill(size - s.size)(empty)

  private def longZip[T, U](a: Seq[T], emptyA: T)(b: Seq[U], emptyB: U) = {
    val maxSize = a.size max b.size
    val ea = expandSeq(a, emptyA, maxSize)
    val eb = expandSeq(b, emptyB, maxSize)
    (ea, eb).zipped
  }

  private val firsts = data.foldLeft(Seq.empty[BestParticipantData]){ (fastestSoFar, participant) =>
    longZip[BestParticipantData, Option[PartTimeInfo]](
      fastestSoFar, BestParticipantData.Empty
    )(
      participant.partTimes.map(Some(_)), None
    ).map{(fastestParticipantSoFar, current) => fastestParticipantSoFar.merge(current)}
  }

  /*DurationRenderer.asInstanceOf[js.Dynamic].prototype.createTicks = (((th: js.Dynamic, plot: js.Dynamic) => {
    th._ticks
    dom.alert("plot: "+plot)
    ()
  }): js.ThisFunction)*/

  private final implicit class RichParticipant(row: Participant){
    def checkboxId = s"part-${row.id}-checkbox"
    def trId = s"part-${row.id}-tr"
  }

  private val Genders = Map[Gender, String](Gender.Male -> "♂", Gender.Female -> "♀")

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

  private val FirstBadge = div(cls := "label label-success")("1.")
  val EmptyHtml: Frag = ""
  private val renderer = new TableRenderer[Participant](
    headRows = 2,
    tableModifiers = Seq(`class` := "table table-condensed table-hover"),
    trWrapper = {(tableRow, row) => tableRow(id := row.trId)}
  )(Seq[Column[Participant]](
    //Column("#")(_.id.toString),
    Column(TableHeadCell("id, jméno", rowCount = 2))((r: Participant) => Seq(
      label(`for` := r.checkboxId, cls := "participant-header-label")(
        r.orderOption.fold(span(cls:="label label-danger label-result")("DNF"))(order => span(cls:="label label-success label-result")(s"$order.")),
        input(`type` := "checkbox", `class` := "participant-checkbox hidden-print", id := r.checkboxId, onchange := { e: Event =>
          val el = e.currentTarget.asInstanceOf[HTMLInputElement]
          //val tableRow = findParent(el, "tr").asInstanceOf[HTMLTableRowElement]
          el.checked match {
            case true => selection.addRow(r)
            case false => selection.removeRow(r)
          }
          //dom.console.log("checkbox changed", el.checked, tableRow)
        }),
        " ",
        r.id + ": " + r.fullNameWithNick
      ),
      div(`class` := "actions hidden-print")(chartButtons(r))
    ))(className = "participant-header"),
    Column[Participant]("Kat.")(p => Seq(Genders(p.gender), " ", p.age))
  ) ++ parts.zipWithIndex.flatMap{case (part, i) =>
    def partData(row: Participant) = row.partTimes.lift(i)
    val best = try{
      firsts(i)
    }catch{
      case e: ArrayIndexOutOfBoundsException =>
        dom.console.warn(s"It seems that nobody has reached part #$i")
        BestParticipantData.Empty
    }
    val firstCell = if(i == 0) TableHeadCell("Start") else TableHeadCell.Empty
    Seq[Column[Participant]](
      Column(firstCell, TableHeadCell("|=>"))((r: Participant) =>
        partData(r).fold[Frag]("–")(pti => Seq(
          pti.startTime.timeOnlyDiv,
          if(best.hasBestStartTime(pti)) FirstBadge else EmptyHtml
        ))
      )(className = "col-start"),
      Column(
        TableHeadCell(span(`class` := "track-length", formatLength(part.trackLength))),
        TableHeadCell(s"čas")
      )((r: Participant) =>
        partData(r).collect{case f: Finished => f}.fold[Frag]("–")(pti => Seq[Frag](
          div(pti.intervalTime.toString),
          if(best.hasBestDuration(pti)) FirstBadge else EmptyHtml
        ))
      )(className = "col-time"),
      Column(
        TableHeadCell(Seq[Frag](part.place, br, span(`class` := "track-length", formatLength(part.cumulativeTrackLength))), colCount = 2),
        TableHeadCell(span(title := s"Čas příchodu na stanoviště č. ${i+1} – ${part.place}", "=>|"))
      ){(r: Participant) =>
        partData(r).collect { case f: Finished => f }.fold("–": Frag){pti => Seq(
          pti.endTime.timeOnlyDiv,
          if(best.hasBestEndTime(pti)) FirstBadge else EmptyHtml
        )}
      }(className = "col-end")
    )
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

  private def formatLength(length: BigDecimal, space: String = " ") = length.toString().replace('.', ',') + space + "km"

  private val tableElement = renderer.renderTable(data)

  private val selectedParticipantsElement = span(`class` := "selected-participants")("–").render

  private val barElement = div(id := "button-bar")(
    button("×", `class` := "close")(onclick := {e: Event => selection.clear() }),
    dropdownGroup("Porovnat vybrané účastníky…", cls:="btn btn-primary dropdown-toggle")(
      for(plot <- Plots) yield chartButton(s"Porovnat ${plot.nameAccusative}", selection().toSeq, plot.generator, s"Porovnat ${plot.nameAccusative}")
    )(cls := "btn-group dropup"),
    //chartButton("Porovnat vybrané účastníky", selection().toSeq, generateWalkPlotData, "Porovnat vybrané účastníky")(`class` := "btn btn-primary"),
    selectedParticipantsElement
  ).render

  private val globalStats = div(id := "global-stats")(
    button("Porovnání startu a času")(cls := "btn btn-default hidden-print")(onclick := {e: Event =>
      val (dialog, jqModal, modalBodyId) = modal("Porovnání startu a času")
      jqModal.on("shown.bs.modal", {() => globalStatsPlot(modalBodyId, data.filter(p => p.hasFinished))})
      dom.document.body.appendChild(dialog)
      jqModal.modal(js.Dictionary("keyboard" -> true))

    })
  ).render

  @tailrec
  private def findParent(el: Node, nodeName: String): Node = el.parentNode match{
    case null => sys.error("Node not found")
    case parent if parent.nodeName.toLowerCase == nodeName.toLowerCase => parent
    case parent => findParent(parent, nodeName)
  }

  private def showBar(implicit x: Nothing): Nothing = ???

  private def showBar_=(shown: Boolean): Unit ={
    barElement.style.visibility = if(shown) "" else "hidden";
  }

  final private case class PlotLine(row: Participant, label: String, points: js.Array[js.Array[_]]) {
    def seriesOptions = js.Dictionary(
      "label" -> label,
      "highlighter" -> Dictionary(
        "formatString" -> (row.id+": %s|%s|%s|%s")
      )
    )
  }

  implicit private class RichTime(moment: Moment){
    @deprecated
    def timeOnly = span(title:=moment.toString)(f"${moment.hours()}:${moment.minutes}%02d")
    def timeOnlyDiv = div(title:=moment.toString)(f"${moment.hours()}:${moment.minutes}%02d")
  }

  private def previousPartCummulativeLengths = Seq(BigDecimal(0)) ++ parts.map(_.cumulativeTrackLength)

  private def processTimes(participant: Participant): PlotLine = {
    val data: Seq[(Moment, BigDecimal)] = (participant.partTimes, parts, previousPartCummulativeLengths).zipped.flatMap{(ti, pi, prev) => Seq(
      Some((ti.startTime, prev)),
      ti.endTimeOption.map( endTime => (endTime, pi.cumulativeTrackLength))
    ).flatten}
    PlotLine(
      row = participant,
      label = participant.id+": "+participant.fullName,
      points = js.Array(data.map{case (x, y) => js.Array(x.toString, y.toDouble)}: _*)
    )
  }

  private def getMates(row: Participant): Seq[Participant] = data.filter{other =>
    (other != row) && (row.finishedPartTimes, other.finishedPartTimes).zipped.exists((myTime, otherTime) => myTime crosses otherTime)
  }

  private def globalStatsPlot(modalBodyId: String, rowsLoader: => Seq[Participant]): Unit ={
    val finishers = rowsLoader.groupBy(p => (p.startTime.toString, p.partTimes.last.endTimeOption.get - p.startTime))
    // .map(p => (p.startTime, p.partTimes.last.endTimeOption.get - p.startTime, p)
    import com.example.moment._
    val plotParams = js.Dictionary(
      "title" -> "Porovnání startu a času chůze",
      "seriesDefaults" -> js.Dictionary(
        //"linePattern" -> "none",
        //"showMarker" -> true,
        //"markerOptions" -> Dictionary("style" -> "diamond"),
        "renderer" -> dom.window.asInstanceOf[js.Dynamic].$.jqplot.BubbleRenderer,
        "rendererOptions" -> js.Dictionary(
          "bubbleGradients" -> true
        ),
        "shadow" -> true
      ),
      //"series" -> series,
      /*"highlighter" -> js.Dictionary(
        "show" -> true,
        "showTooltip" -> true,
        "formatString" -> "%s|%s|%s|%s",
        "bringSeriesToFront" -> true
      ),*/
      "height" -> 500,
      "legend" -> Dictionary("show" -> true),
      "axes" -> Dictionary(
        "xaxis" -> Dictionary(
          "renderer" -> dom.window.asInstanceOf[js.Dynamic].$.jqplot.DateAxisRenderer,
          "tickOptions" -> Dictionary("formatString" -> "%#H:%M"),
          "min" -> moment(startTime).minutes(0).toString,
          "tickInterval" -> "5 minutes"
        ),
        "yaxis" -> Dictionary(
          "renderer" -> DurationRenderer,
          "tickOptions" -> Dictionary(
            "formatString" -> "aaa %#H:%M",
            "formatter" -> ((format: String, value: Moment) => value.toString)
          ),
          //"min" -> moment(startTime).minutes(0).toString,
          "tickInterval" -> "30 minutes"
          //"max" -> parts.last.cumulativeTrackLength.toDouble,
        )
      )
    )
    //dom.console.log(zeroMoment.toString)
    val plotPoints = js.Array(js.Array(finishers.map{case ((moment, time), participants) => js.Array(moment/*.hours()*60+moment.minutes()*/, zeroMoment.add({dom.console.log(time+" – "+participants); time+1}, "milliseconds").toString, math.sqrt(participants.size.toDouble), participants.map(_.fullName).mkString(", ") + " ("+participants.size+")")}.toSeq: _*))
    dom.console.log("plotPoints", plotPoints)
    //dom.alert(finishers.mkString("\n"))

    dom.window.asInstanceOf[js.Dynamic].$.jqplot(modalBodyId, plotPoints, plotParams)


  }

  private val DateAxisRenderer = dom.window.asInstanceOf[js.Dynamic].$.jqplot.DateAxisRenderer

  private def generateWalkPlotData(rowsLoader: Seq[Participant]) = {
    import com.example.moment._
    val data = rowsLoader.map(processTimes)
    val series = js.Array(data.map(line =>
      line.seriesOptions
    ): _*)
    val plotPoints = js.Array(data.map(_.points): _*)
    PlotData(
      plotPoints = plotPoints,
      plotParams = js.Dictionary(
        "title" -> "Chůze účastníka",
        "seriesDefaults" -> js.Dictionary(
          "linePattern" -> "dashed",
          "showMarker" -> true,
          "markerOptions" -> Dictionary("style" -> "diamond"),
          "shadow" -> false
        ),
        "series" -> series,
        "highlighter" -> js.Dictionary(
          "show" -> true,
          "showTooltip" -> true,
          "formatString" -> "%s|%s|%s|%s",
          "bringSeriesToFront" -> true
        ),
        "height" -> 500,
        "legend" -> Dictionary("show" -> true),
        "axes" -> Dictionary(
          "xaxis" -> Dictionary(
            "renderer" -> DateAxisRenderer,
            "tickOptions" -> Dictionary("formatString" -> "%#H:%M"),
            "min" -> moment(startTime).minutes(0).toString,
            "tickInterval" -> "1 hours"
          ),
          "yaxis" -> Dictionary(
            "min" -> 0,
            "max" -> parts.last.cumulativeTrackLength.toDouble,
            "tickInterval" -> 10
          )
        )
      )
    )
  }

  private def generateSpeedPlotData(rows: Seq[Participant]) = {
    val data = rows.map{p =>
      PlotLine(row = p, label = p.fullName, points = js.Array(
        (p.partTimes, parts).zipped.flatMap((partTime, part) => partTime.durationOption.map { duration =>
          dom.console.log("part.trackLength, (duration.toDouble/1000/3600)", part.trackLength.toDouble, (duration.toDouble/1000/3600))
          dom.console.log("part.trackLength / (duration.toDouble / 1000 / 3600)", (part.trackLength / (duration.toDouble / 1000 / 3600)).toString)
          js.Array(part.cumulativeTrackLength.toDouble, part.trackLength.toDouble / (duration.toDouble / 1000 / 3600))
        }): _*)
      )
    }
    val series = js.Array(data.map(_.seriesOptions): _*)
    val plotPoints = js.Array(data.map(_.points): _*)
    dom.console.log("plotPoints", plotPoints)
    PlotData(
      plotPoints = plotPoints,
      plotParams = js.Dictionary(
        "title" -> "Rychlost účastníka",
        "seriesDefaults" -> js.Dictionary(
          "renderer" -> BarRenderer,
          "rendererOptions" -> js.Dictionary(
            "barWidth" -> 10
          ),
          "pointLabels" -> true,
          "showMarker" -> true
        ),
        "series" -> series,
        "height" -> 500,
        "legend" -> Dictionary("show" -> true)
      )
    )
  }

  private def generatePausesPlotData(rows: Seq[Participant]) = {
    val data = rows.map{p =>
      PlotLine(row = p, label = p.fullName, points = js.Array((p.pauses, parts).zipped.map((pause, part) => js.Array(part.cumulativeTrackLength.toDouble, pause/1000/60)): _*))
    }
    dom.console.log("rows.head.pauses = " + rows.head.pauses.toIndexedSeq.toString)
    val series = js.Array(data.map(_.seriesOptions): _*)
    val plotPoints = js.Array(data.map(_.points): _*)
    dom.console.log("plotPoints", plotPoints)
    PlotData(
      plotPoints = plotPoints,
      plotParams = js.Dictionary(
        "title" -> "Pauzy účastníka",
        "seriesDefaults" -> js.Dictionary(
          "renderer" -> BarRenderer,
          "rendererOptions" -> js.Dictionary(
            "barWidth" -> 10
          ),
          "pointLabels" -> true,
          //"linePattern" -> "dashed",
          "showMarker" -> true
          //"markerOptions" -> Dictionary("style" -> "diamond"),
          //"shadow" -> false
        ),
        "series" -> series,
        /*"highlighter" -> js.Dictionary(
          "show" -> true,
          "showTooltip" -> true,
          "formatString" -> "%s|%s|%s|%s",
          "bringSeriesToFront" -> true
        ),*/
        "height" -> 500,
        "legend" -> Dictionary("show" -> true),
        "axes" -> Dictionary(
          /*"yaxis" -> Dictionary(
            //"renderer" -> DateAxisRenderer,
            //"tickOptions" -> Dictionary("formatString" -> "%#H:%M"),
            "min" -> 0
            //"tickInterval" -> "1 hours"
          ),
          "xaxis" -> Dictionary(
            "min" -> 0,
            "max" -> parts.last.cumulativeTrackLength.toDouble,
            "tickInterval" -> 10
          )*/
        )
      )
    )
  }

  private def initializePlot(modalBodyId: String, data: PlotData): Unit ={
    //dom.console.log("times", js.Array(processTimes(row)))
    //dom.console.log("plotParams", plotParams)
    dom.window.asInstanceOf[js.Dynamic].$.jqplot(modalBodyId, data.plotPoints, data.plotParams)
    dom.window.asInstanceOf[js.Dynamic].$("#"+modalBodyId).on("jqplotDataClick", {(ev: js.Any, seriesIndex: js.Any, pointIndex: js.Any, data: js.Any) =>
      dom.console.log("click", ev, seriesIndex, pointIndex, data)
    })
  }

  private def modal(title: Frag) = {
    val modalBodyId = IdGenerator.newId()
    val modalHeader = div(`class`:="modal-header")(button(`type`:="button", `class`:="close", "data-dismiss".attr := "modal")(span("aria-hidden".attr := "true")("×")))(h4(`class`:="modal-title")(title))
    var modalBody = div(`class`:="modal-body", id := modalBodyId)
    var modalFooter = div(`class`:="modal-footer")
    var modalDialog = div(`class`:="modal-dialog modal-xxl")(div(`class`:="modal-content")(modalHeader, modalBody, modalFooter))
    var dialog = div(`class`:="modal fade")(modalDialog).render

    val jqModal = dom.window.asInstanceOf[js.Dynamic].$(dialog)
    jqModal.on("hidden.bs.modal", {() => dialog.parentNode.removeChild(dialog)})
    (dialog, jqModal, modalBodyId)
  }

  private def chartButton(title: String, rowsLoader: => Seq[Participant], plotDataGenerator: Seq[Participant] => PlotData, description: Frag) = button(`class` := "btn btn-default btn-xs")(description)(onclick := {_:Any =>
    val (dialog, jqModal, modalBodyId) = modal(title)
    jqModal.on("shown.bs.modal", {() => initializePlot(modalBodyId, plotDataGenerator(rowsLoader))})
    dom.document.body.appendChild(dialog)
    jqModal.modal(js.Dictionary("keyboard" -> true))
  })


  private def dropdownGroup(mods: Modifier*)(buttons: Frag*) = div(cls:="btn-group")(
    button(cls:="btn btn-normal dropdown-toggle", "data-toggle".attr := "dropdown", "aria-haspopup".attr := "true", "aria-expanded".attr := "false")(mods: _*),
    div(cls:="dropdown-menu")(buttons : _*)
  )


  private def chartButtons(row: Participant) = Seq[Frag](
    for(plot <- Plots) yield     chartButton(
      s"Graf ${plot.nameGenitive} pro ${row.fullNameWithNick}",
      Seq(row),
      plot.generator,
      Seq(span(`class`:=s"glyphicon glyphicon-${plot.glyphiconName}", "aria-hidden".attr := "true"))
    ),
    /*chartButton(
      s"Graf chůze pro ${row.fullNameWithNick}",
      Seq(row), generateWalkPlotData,
      Seq(span(`class`:="glyphicon glyphicon-user", "aria-hidden".attr := "true"))
    ),
    chartButton(
      s"Graf pauz pro ${row.fullNameWithNick}",
      Seq(row), generatePausesPlotData,
      Seq(span(`class`:="glyphicon glyphicon-pause", "aria-hidden".attr := "true"))
    ),*/
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

  private def addSeparators[T](separator: T)(s: Seq[T]): Seq[T] = (Stream.continually(separator), s).zipped.flatMap{(x, y) => Seq(x, y)}.drop(1)

  private def causeStream(e: Throwable): Stream[Throwable] = Stream.cons(e, Option(e.getCause).fold(Stream.empty[Throwable])(causeStream))

  private def showCells(cells: Seq[String]): Frag = addSeparators[Frag](", ")(cells.map(c => code(c)))

  private def showThrowable(rootThrowable: Throwable) = ul(causeStream(rootThrowable).map {
    case CellsParsingException(data, _) => li("K chybě došlo při zpracování následujících buňek: ", showCells(data))
    case BadTimeInfoFormatException() => li("Očekáváné varianty: a) nevyplněno, b) pouze čas startu, c) všechny tři časy (start, doba, cíl). Pokud je některý čas nevyplněn, očekává se prázdné políčko nebo \"X\".")
    case MaxHourDeltaExceededException(maxHourDelta, prevTime, currentTime) => li(f"Od ${prevTime.hoursAndMinutes} do ${currentTime.hoursAndMinutes} to trvá více než $maxHourDelta hodin, což je nějaké divné, asi to bude chyba.")
    case e: DeadlineExceededException => li("Tento účastník došel až po konci pochodu.")
    case e => li(i(e.getClass.getName), ": ", e.getMessage)
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
    //updateHeadHeight()
    dom.window.asInstanceOf[js.Dynamic].$(tableElement).stickyTableHeaders(Dictionary("cacheHeaderHeight" -> true, "fixedOffset" -> 0))
    //dom.window.asInstanceOf[js.Dynamic].$(tableElement).sticky()// Dictionary("columnCount" -> 4) )
    if (enableHorizontalStickyness) {
      addHorizontalStickyness()
    }
  }

  def addHorizontalStickyness(): Unit = {
    val participantHeaders = dom.window.asInstanceOf[js.Dynamic].Array.prototype.slice.call(tableElement.querySelectorAll(".participant-header")).asInstanceOf[js.Array[HTMLElement]]
    for(h <- participantHeaders){
      h.style.position = "relative"
    }
    val horizontalScrollClassGuard = ValueGuard(false){ (_, scrolledHorizontally) =>
      if(scrolledHorizontally){
        for(t <- participantHeaders){
          t.classList.add("scrolled-horizontally")
        }
      }else{
        for(t <- participantHeaders){
          t.classList.remove("scrolled-horizontally")
        }
      }
    }
    val horizontalScrollPositionGuard = ValueGuard(0.0){(_, scrollLeft) =>
      val left = s"${scrollLeft}px"
      for(t <- participantHeaders){
        t.style.left = left
      }
      horizontalScrollClassGuard.update(scrollLeft != 0)
    }
    dom.document.addEventListener("scroll", (_: Event) => {
      horizontalScrollPositionGuard.update(dom.document.documentElement.scrollLeft)
    })
  }

  /*private def updateHeadHeight(): Unit ={
    val theadElement = tableElement.getElementsByTagName("thead").item(0).asInstanceOf[HTMLElement]
    theadElement.style.minHeight = tableElement.offsetHeight+"px"
  }*/

}
