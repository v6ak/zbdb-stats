package com.v6ak.zbdb

import com.example.moment._
import com.v6ak.zbdb.`$`.jqplot.DateAxisRenderer
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.Dictionary

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

final class PlotRenderer(participantTable: ParticipantTable) {

  import participantTable._

  val Plots = Seq(
    ParticipantPlotGenerator("chůze", "chůzi", "globe", generateWalkPlotData),
    ParticipantPlotGenerator("rychlosti", "rychlost", "play", generateSpeedPlotData),
    ParticipantPlotGenerator("pauz", "pauzy", "pause", generatePausesPlotData)
  )

  private def zeroMoment = moment("2000-01-01") // We don't want to mutate it

  private val DurationRenderer: js.ThisFunction = (th: js.Dynamic) => {
    dom.window.asInstanceOf[js.Dynamic].$.jqplot.DateAxisRenderer.call(th)
  }
  DurationRenderer.asInstanceOf[js.Dynamic].prototype = new DateAxisRenderer()
  DurationRenderer.asInstanceOf[js.Dynamic].prototype.init = ((th: js.Dynamic) => {
    dom.window.asInstanceOf[js.Dynamic].$.jqplot.DateAxisRenderer.prototype.init.call(th)
    th.tickOptions.formatter = ((format: String, value: Moment) => {
      val diff = value - zeroMoment
      val hours = diff/1000/60/60
      val minutes = diff/1000/60 - hours*60
      f"$hours%02d:$minutes%02d"
    }): js.Function
  }): js.ThisFunction

  private val BarRenderer = dom.window.asInstanceOf[js.Dynamic].$.jqplot.BarRenderer

  private val DateAxisRenderer = dom.window.asInstanceOf[js.Dynamic].$.jqplot.DateAxisRenderer

  def generateWalkPlotData(rowsLoader: Seq[Participant]) = {
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

  def startTimeToTotalDurationPlot(modalBodyId: String, rowsLoader: => Seq[Participant]): Unit ={
    val finishers = rowsLoader.groupBy(p => (p.startTime.toString, p.partTimes.last.endTimeOption.get - p.startTime))
    import com.example.moment._
    val plotParams = js.Dictionary(
      "title" -> "Porovnání startu a času chůze",
      "seriesDefaults" -> js.Dictionary(
        "renderer" -> dom.window.asInstanceOf[js.Dynamic].$.jqplot.BubbleRenderer,
        "rendererOptions" -> js.Dictionary(
          "bubbleGradients" -> true
        ),
        "shadow" -> true
      ),
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
          "tickInterval" -> "30 minutes"
        )
      )
    )
    val plotPoints = js.Array(js.Array(finishers.map{case ((moment, time), participants) => js.Array(moment/*.hours()*60+moment.minutes()*/, zeroMoment.add({dom.console.log(time+" – "+participants); time+1}, "milliseconds").toString, math.sqrt(participants.size.toDouble), participants.map(_.fullName).mkString(", ") + " ("+participants.size+")")}.toSeq: _*))
    dom.window.asInstanceOf[js.Dynamic].$.jqplot(modalBodyId, plotPoints, plotParams)
  }

  private def generateSpeedPlotData(rows: Seq[Participant]) = {
    val data = rows.map{p =>
      PlotLine(row = p, label = p.fullName, points = js.Array(
        (p.partTimes, parts).zipped.flatMap((partTime, part) => partTime.durationOption.map { duration =>
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

}
