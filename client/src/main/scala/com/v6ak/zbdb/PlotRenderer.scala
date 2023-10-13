package com.v6ak.zbdb

import com.example.moment.{moment, _}
import com.v6ak.zbdb.`$`.jqplot.DateAxisRenderer
import org.scalajs.dom
import org.scalajs.dom._

import scala.collection.immutable
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.annotation._
import Bootstrap.DialogUtils
import com.example.moment._

@JSGlobal @js.native object `$` extends js.Object{
  @js.native object jqplot extends js.Object{
    @js.native class LinearAxisRenderer extends js.Object{
      def createTicks(plot: js.Dynamic): Unit = js.native
    }
    @js.native class DateAxisRenderer extends LinearAxisRenderer{
      override def createTicks(plot: js.Dynamic): Unit = js.native
    }
  }
}

final class PlotRenderer(participantTable: ParticipantTable) {
  import ChartJsUtils._

  import participantTable._

  val Plots = Seq(
    ParticipantPlotGenerator("chůze", "chůzi", Glyphs.Pedestrian, generateWalkPlotData),
    ParticipantPlotGenerator("rychlosti", "rychlost", Glyphs.Play, generateSpeedPlotData),
    ParticipantPlotGenerator("pauz", "pauzy", Glyphs.Pause, generatePausesPlotData)
  )

  val GlobalPlots = Seq[(String, (Option[String], ((String, =>Seq[Participant], ParticipantTable) => Unit)))](
    "Porovnání startu a času" -> startTimeToTotalDurationPlot,
    "Genderová struktura" -> genderStructurePlot,
    "Počet lidí" -> (None -> remainingParticipantsCountPlot _),
    "Počet lidí v %" -> (None -> remainingRelativeCountPlot _)
  )

  private val GenderNames = Map[Gender, String](
    Gender.Male -> "muž",
    Gender.Female -> "žena"
  )

  private val GenderColors = Map[Gender, String](
    Gender.Female -> "#FFC0CB",
    Gender.Male -> "#008080",
  )

  private val BarRenderer = dom.window.asInstanceOf[js.Dynamic].$.jqplot.BarRenderer

  private val DateAxisRenderer = dom.window.asInstanceOf[js.Dynamic].$.jqplot.DateAxisRenderer

  def generateWalkPlotData(rowsLoader: Seq[Participant]) = {
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

  private val startTimeToTotalDurationRadius = (context: js.Dynamic) => {
    // It doesn't seem to be recalculated when resized, even though this function is called
    val baseSize = math.max(
      5, // minimal base size in px
      math.min(
        context.chart.height.asInstanceOf[Double] / 20,
        context.chart.width.asInstanceOf[Double] / 20,
      )
    )
    val res = baseSize
    // area should scale linearly, so we need sqrt for radius
    res * math.sqrt(context.raw.participants.length.asInstanceOf[Int])
  }

  private def startTimeToTotalDurationTooltip = {
    literal(
      callbacks = literal(
        label = (context: js.Dynamic) => {
          val participants = context.raw.participants.asInstanceOf[js.Array[Participant]]
          participants.map(_.fullName).mkString(", ") + " (" + participants.size + ")"
        },
      ),
    )
  }

  private def startTimeToTotalDurationPlot = showChartInModal(
    title = "Porovnání startu a času (pouze finalisti)"
  ) { rows =>
    val finishers = rows.filter(p => p.hasFinished).groupBy(p =>
      (p.startTime.toString, p.partTimes.last.endTimeOption.get - p.startTime)
    )
    literal(
      `type` = "bubble",
      data = literal(
        datasets = js.Array(
          literal(
            radius = startTimeToTotalDurationRadius,
            data = finishers.map { case ((_startTimeString, time), participants) =>
              val startTime = participants(0).startTime
              literal(
                x = startTime,
                y = zeroMoment.add(time, "milliseconds"),
                participants = participants.toJSArray
              )
            }.toJSArray
          )
        )
      ),
      options = literal(
        scales = literal(
          x = timeAxis("Čas startu"),
          y = durationAxis(
            label="Celková doba",
            min=zeroMoment.add(
              finishers.values.flatten.map(p =>
                (p.partTimes.last.endTimeOption.get - p.startTime) / 3600 / 1000
              ).min - 1,
              "hours"
            )
          ),
        ),
        plugins = literal(
          legend = literal(display = false),
          tooltip = startTimeToTotalDurationTooltip,
        ),
      ),
    )
  }

  private def computeCumulativeMortality(rows: Seq[Participant]) = {
    val mortalityMap = rows.map(_.partTimes.count(_.hasEndTime)).groupBy(identity).mapValues(_.size).map(identity).toMap
    val mortalitySeq = (0 to mortalityMap.keys.max).map(mortalityMap.getOrElse(_, 0))
    mortalitySeq.scan(0)(_ + _).tail
  }

  def remainingParticipantsCountPlot(modalBodyId: String, rowsLoader: => Seq[Participant], participantTable: ParticipantTable): Unit = {
    val rows = rowsLoader
    val cummulativeMortality: immutable.IndexedSeq[Int] = computeCumulativeMortality(rows)
    val data = cummulativeMortality.dropRight(1).zipWithIndex.map{case (cm, i) => (participantTable.parts(i), cm, rows.size - cm, i)}
    val ticks = js.Array(data.map { case (header, _, _, i) =>
      s"${i+1}. (${header.cumulativeTrackLength} km)"
    }: _*)
    val plotParams = js.Dictionary(
      "title" -> "Počet lidí",
      "seriesDefaults" -> js.Dictionary(
        "showMarker" -> true,
        "markerOptions" -> Dictionary("style" -> "diamond")
      ),
      "highlighter" -> js.Dictionary(
        "show" -> true,
        "showTooltip" -> true,
        "formatString" -> "%2$s",
        "bringSeriesToFront" -> true
      ),
      "axes" -> js.Dictionary(
        "xaxis" -> js.Dictionary(
          "renderer" -> dom.window.asInstanceOf[js.Dynamic].$.jqplot.CategoryAxisRenderer,
          "ticks" -> ticks
        )
      ),
      "height" -> 500
    )
    val plotPoints = js.Array(
      js.Array(data.map(_._2): _*),
      js.Array(data.map(_._3): _*)
    )
    dom.window.asInstanceOf[js.Dynamic].$.jqplot(modalBodyId, plotPoints, plotParams)
  }

  def remainingRelativeCountPlot(modalBodyId: String, rowsLoader: => Seq[Participant], participantTable: ParticipantTable): Unit = {
    val rows = rowsLoader
    val cummulativeMortality: immutable.IndexedSeq[Int] = computeCumulativeMortality(rows)
    val size = rows.size
    val data = cummulativeMortality.dropRight(1).zipWithIndex.map{case (cm, i) => (i, 100.0*cm/size, 100.0*(size - cm)/size)}
    val ticks = js.Array(data.map{case (i, _, _) =>
      val header = participantTable.parts(i)
      s"${i+1}. (${header.cumulativeTrackLength} km)"
    }: _*)
    val plotParams = js.Dictionary(
      "title" -> "Počet lidí v %",
      "seriesDefaults" -> js.Dictionary(
        "showMarker" -> true,
        "renderer" -> BarRenderer,
        "markerOptions" -> Dictionary("style" -> "diamond")
      ),
      "highlighter" -> js.Dictionary(
        "show" -> true,
        "showTooltip" -> true,
        "formatString" -> "%2$s",
        "bringSeriesToFront" -> true
      ),
      "axes" -> js.Dictionary(
        "xaxis" -> js.Dictionary(
          "renderer" -> dom.window.asInstanceOf[js.Dynamic].$.jqplot.CategoryAxisRenderer,
          "ticks" -> ticks
        )
      ),
      "height" -> 500
    )
    val plotPoints = js.Array(
      js.Array(data.map(_._2): _*),
      js.Array(data.map(_._3): _*)
    )
    dom.window.asInstanceOf[js.Dynamic].$.jqplot(modalBodyId, plotPoints, plotParams)
  }

  def genderStructurePlot = showChartInModal() { rows =>
    val structure = rows.groupBy(_.gender)
    js.Dynamic.literal(
      `type` = "pie",
      data = dataFromTriples(structure.toSeq.map { case (gender, p) =>
        (GenderNames(gender), p.size, GenderColors(gender))
      }),
      title = "Genderová struktura startujících",
    )
  }

  def ageStructurePlot(modalBodyId: String, rowsLoader: => Seq[Participant], participantTable: ParticipantTable): Unit ={
    val structure = rowsLoader.groupBy(_.birthYear).toIndexedSeq.sortBy(_._1)
    val ticks = js.Array(structure.map(_._1.get): _*)
    val plotParams = js.Dictionary(
      "title" -> "Věková struktura",
      "seriesDefaults" -> js.Dictionary(
        "renderer" -> BarRenderer,
        "pointLabels" -> js.Dictionary(
          "show" -> true
        ),
        "shadow" -> true
      ),
      "axes" -> js.Dictionary(
        "xaxis" -> js.Dictionary(
          "renderer" -> dom.window.asInstanceOf[js.Dynamic].$.jqplot.CategoryAxisRenderer,
          "ticks" -> ticks
        ),
        "yaxis" -> js.Dictionary(
          "tickOptions" -> js.Dictionary(
            "formatString"-> "%.0f"
          )
        )
      ),
      "height" -> 500,
      "legend" -> Dictionary("show" -> true)
    )
    val plotPoints = js.Array(js.Array(structure.map{_._2.size}: _*))
    dom.window.console.log("plotPoints", ticks, plotPoints)
    dom.window.asInstanceOf[js.Dynamic].$.jqplot(modalBodyId, plotPoints, plotParams)
  }

  private def generateSpeedPlotData(rows: Seq[Participant]) = {
    val data = rows.map{p =>
      PlotLine(row = p, label = p.fullName, points = js.Array(
        (p.partTimes lazyZip parts).flatMap((partTime, part) => partTime.durationOption.map { duration =>
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
      PlotLine(row = p, label = p.fullName, points = js.Array((p.pauseTimes, parts).zipped.map((pause, part) => js.Array(part.cumulativeTrackLength.toDouble, pause/1000/60)): _*))
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
          "showMarker" -> true
        ),
        "series" -> series,
        "height" -> 500,
        "legend" -> Dictionary("show" -> true)
      )
    )
  }

  def initializePlot(modalBodyId: String, data: PlotData): Unit ={
    dom.window.asInstanceOf[js.Dynamic].$.jqplot(modalBodyId, data.plotPoints, data.plotParams)
    dom.window.asInstanceOf[js.Dynamic].$("#"+modalBodyId).on("jqplotDataClick", {(ev: js.Any, seriesIndex: js.Any, pointIndex: js.Any, data: js.Any) =>
      dom.console.log("click", ev, seriesIndex, pointIndex, data)
    })
  }

  private def processTimes(participant: Participant): PlotLine = {
    val data: Seq[(Moment, BigDecimal)] = (participant.partTimes lazyZip parts lazyZip previousPartCummulativeLengths)
      .flatMap{(ti, pi, prev) =>
        Seq(
          Some((ti.startTime, prev)),
          ti.endTimeOption.map( endTime => (endTime, pi.cumulativeTrackLength))
        ).flatten
      }
    PlotLine(
      row = participant,
      label = s"${participant.id}: ${participant.fullName}",
      points = js.Array(data.map{case (x, y) => js.Array(x.toString, y.toDouble)}: _*)
    )
  }

}
