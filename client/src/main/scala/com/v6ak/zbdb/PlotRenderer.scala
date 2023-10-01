package com.v6ak.zbdb

import com.example.moment._
import com.v6ak.zbdb.`$`.jqplot.DateAxisRenderer
import org.scalajs.dom

import scala.collection.immutable
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.scalajs.js
import scala.scalajs.js.annotation._

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

  import participantTable._

  val Plots = Seq(
    ParticipantPlotGenerator("chůze", "chůzi", "globe", generateWalkPlotData),
    ParticipantPlotGenerator("rychlosti", "rychlost", "play", generateSpeedPlotData),
    ParticipantPlotGenerator("pauz", "pauzy", "pause", generatePausesPlotData)
  )

  val GlobalPlots = Seq(
    "Porovnání startu a času" -> startTimeToTotalDurationPlot _,
    "Genderová struktura" -> genderStructurePlot _
  ) ++
    (if(participantTable.formatVersion.ageType == AgeType.BirthYear) Seq("Věková struktura" -> ageStructurePlot _) else Seq()) ++ Seq(
    "Počet lidí" -> remainingParticipantsCountPlot _,
    "Počet lidí v %" -> remainingRelativeCountPlot _
  )

  private val GenderNames = Map[Gender, String](
    Gender.Male -> "muž",
    Gender.Female -> "žena"
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

  def startTimeToTotalDurationPlot(modalBodyId: String, rowsLoader: => Seq[Participant], participantTable: ParticipantTable): Unit ={
    val finishers = rowsLoader.filter(p => p.hasFinished).groupBy(p => (p.startTime.toString, p.partTimes.last.endTimeOption.get - p.startTime))
    import com.example.moment._
    val plotParams = js.Dictionary(
      "title" -> "Porovnání startu a času chůze (pouze finalisti)",
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
    val plotPoints = js.Array(
      js.Array(
        finishers.map{case ((moment, time), participants) =>
          js.Array(
            moment/*.hours()*60+moment.minutes()*/,
            zeroMoment.add({dom.console.log(time+" – "+participants); time+1}, "milliseconds").toString,
            math.sqrt(participants.size.toDouble),
            participants.map(_.fullName).mkString(", ") + " ("+participants.size+")",
          )
        }.toSeq
      :_*)
    )
    dom.window.asInstanceOf[js.Dynamic].$.jqplot(modalBodyId, plotPoints, plotParams)
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

  def genderStructurePlot(modalBodyId: String, rowsLoader: => Seq[Participant], participantTable: ParticipantTable): Unit ={
    val structure = rowsLoader.groupBy(_.gender)
    val plotParams = js.Dictionary(
      "title" -> "Genderová struktura startujících",
      "seriesDefaults" -> js.Dictionary(
        "renderer" -> dom.window.asInstanceOf[js.Dynamic].$.jqplot.PieRenderer,
        "rendererOptions" -> js.Dictionary(
          "showDataLabels" -> true
        ),
        "shadow" -> true
      ),
      "height" -> 500,
      "legend" -> Dictionary("show" -> true)
    )
    val plotPoints = js.Array(js.Array(structure.map{case (gender, p) => js.Array(GenderNames(gender), p.size)}.toSeq: _*))
    dom.window.asInstanceOf[js.Dynamic].$.jqplot(modalBodyId, plotPoints, plotParams)
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
