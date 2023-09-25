package com.v6ak.zbdb

import com.example.RichMoment.toRichMoment
import com.example.moment.{Moment, moment}
import com.v6ak.scalajs.time.TimeInterval
import HtmlUtils._
import TextUtils._
import Bootstrap._
import org.scalajs.dom
import scalatags.JsDom.all.{i => iTag, name => _, _}
import scala.scalajs.js


final case class FullPartInfo(
  previousPartMetaOption: Option[Part],
  partMeta: Part,
  part: PartTimeInfo,
  nextPartOption: Option[PartTimeInfo],
)

final class TimeLineRenderer(participantTable: ParticipantTable, plotRenderer: PlotRenderer) {
  import participantTable._
  import plotRenderer.{Plots, initializePlot}

  private def timePoint(time: Moment, content: Frag, className: String = "") = tr(
    `class` := s"timeline-point $className",
    td(`class` := "timeline-time", time.hoursAndMinutes),
    td(),
    td(`class` := "timeline-content", content),
  )

  private def process(content: Frag, duration: TimeInterval, durationIcon: String, className: String = "") = tr(
    `class` := s"timeline-process $className",
    td(`class` := "timeline-time"),
    td(`class` := "timeline-duration")(
      fseq(
        glyphicon(durationIcon),
        " ",
        duration.toString
      )
    ),
    td(`class` := "timeline-content", content),
  )

  private def end(content: Frag, className: String = "") = tr(
    `class` := s"timeline-end $className",
    td(`class` := "timeline-time"),
    td(`class` := "timeline-duration"),
    td(`class` := "timeline-content", content),
  )

  private def pause(content: Frag, duration: TimeInterval) =
    process(content, duration = duration, durationIcon = "pause", className = "pause")
  private def walk(content: Frag, duration: TimeInterval) =
    process(content, duration = duration, durationIcon = "play", className = "walk")
  private def gaveUp(content: Frag) = end(content, className = "gave-up")
  private def finish(time: Moment, content: Frag) = timePoint(time, content, className = "finish")

  def timeLine(row: Participant) = {
    val prevParts = Seq(None) ++ parts.map(Some(_))
    val nextPartInfos: Seq[Option[PartTimeInfo]] = row.partTimes.drop(1).map(Some(_)) ++ Seq(None)
    div(
      div(`class` := "legend")(
        h2("Legenda"),
        legendTable,
      ),
      table(
        `class` := "timeline",
        (
          prevParts lazyZip
          parts lazyZip
          row.partTimes lazyZip
          nextPartInfos
        ).map(FullPartInfo).zipWithIndex.flatMap{ case (fpi, pos) =>
          partTimeLine(row)(pos, fpi)
        }
      ),
      h2("Vizualizace"),
      chartButtons(row),
    )
  }

  private def legendTable = {
    table(
      `class` := "timeline",
      walk("chůze trvající 4:20", TimeInterval(260)),
      timePoint(moment("2016-01-20 15:15"), "příchod/odchod v 15:15"),
      pause("pauza trvající 10 minut", TimeInterval(10)),
      gaveUp("konec před dosažením cíle"),
      finish(moment("2016-01-20 16:20"), "Dosažení cíle v 16:20"),
    )
  }

  val brief = true

  def verbose(f: Frag) = if(brief) "" else f
  def verbose(s: String) = if(brief) "" else s

  private def partTimeLine(row: Participant)(
    pos: Int,
    fullPartInfo: FullPartInfo
  ): Seq[Frag] = {
    import fullPartInfo._
    import row.gender
    def langGaveUp = gender.inflect("vzdala", "vzdal")
    def langArrived = gender.inflect("dorazila", "dorazil")
    def cumLen: Frag = fseq(" (celkem ", strong(formatLength(partMeta.cumulativeTrackLength)), ")")
    Seq(
      timePoint(
        part.startTime,
        previousPartMetaOption.fold("Start")(pm =>
          verbose(s"${gender.inflect("vyšla", "vyšel")} ${
            s"z $pos. stanoviště ${pm.place}"
          }")
        ),
        className = if (previousPartMetaOption.isEmpty) "start" else ""
      ),
    ) ++ (part match {
      case PartTimeInfo.Finished(_startTime, endTime, intervalTime) =>
        val isFinish = pos == parts.size - 1
        fseq(
          walk(
            fseq(
              strong(formatLength(partMeta.trackLength)), br(),
              "rychlost ",
              strong(f"${partMeta.trackLength * 60 / intervalTime.totalMinutes}%1.2f km/h"),
              " = tempo ",
              strong(f"${intervalTime / partMeta.trackLength} / km"),
            ),
            duration = intervalTime,
          ),
          if(isFinish)
            finish(
              endTime,
              fseq(s"Cíl: ${partMeta.place}", cumLen)
              // TODO: stats
            )
          else
            timePoint(
              endTime,
              verbose(s"$langArrived na ${pos + 1}. stanoviště ${partMeta.place}")
            ),
          nextPartOption.fold[Frag](
            if (isFinish) ""
            else gaveUp(fseq(s"$langGaveUp to na stanovišti ", strong(s"${pos + 1}."), s" ${partMeta.place}", cumLen))
          ) { nextPart =>
            pause(
              fseq(strong(s"${pos+1}."), s" ${partMeta.place}", cumLen),
              duration = TimeInterval((nextPart.startTime - endTime)/60/1000)
            )
          }
        )
      case PartTimeInfo.Unfinished(_startTime) => Seq(
        gaveUp(s"$langGaveUp to při cestě na ${pos+1}. stanoviště.")
      )
    })
  }


  private def chartButtons(row: Participant) = div(`class` := "chart-buttons")(
    for {
      plot <- Plots
      name = s"Graf ${plot.nameGenitive}"
    } yield chartButton(
      name,
      Seq(row),
      plot.generator,
      Seq(span(`class` := s"glyphicon glyphicon-${plot.glyphiconName}", aria.hidden := "true"))
    )(title := name),
  )

  private def chartButton(title: String, rowsLoader: => Seq[Participant], plotDataGenerator: Seq[Participant] => PlotData, description: Frag) =
    button(`class` := "btn btn-default btn-l")(description, " ", title)(onclick := { _: Any =>
      val (dialog, jqModal, modalBodyId) = modal(title)
      jqModal.on("shown.bs.modal", { () => initializePlot(modalBodyId, plotDataGenerator(rowsLoader)) })
      dom.document.body.appendChild(dialog)
      jqModal.modal(js.Dictionary("keyboard" -> true))
    })


}
