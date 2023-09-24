package com.v6ak.zbdb

import com.example.RichMoment.toRichMoment
import com.example.moment.Moment
import com.v6ak.scalajs.time.TimeInterval
import HtmlUtils._
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

  private def timePoint(time: Moment, content: Frag) = tr(
    `class` := "timeline-point",
    td(`class` := "timeline-point-time", time.hoursAndMinutes),
    td(`class` := "timeline-content", content),
  )

  private def process(content: Frag, className: String = "") = tr(
    `class` := s"timeline-process $className",
    td(`class` := "timeline-process-time"),
    td(`class` := "timeline-content", content),
  )

  private def gaveUp(content: Frag) = process(content, className = "gave-up")
  private def pause(content: Frag) = process(content, className = "pause")
  private def finish(content: Frag) = process(content, className = "finish")

  def timeLine(row: Participant) = {
    val prevParts = Seq(None) ++ parts.map(Some(_))
    val nextPartInfos: Seq[Option[PartTimeInfo]] = row.partTimes.drop(1).map(Some(_)) ++ Seq(None)
    div(
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
      chartButtons(row),
    )
  }

  private def partTimeLine(row: Participant)(
    pos: Int,
    fullPartInfo: FullPartInfo
  ): Seq[Frag] = {
    import fullPartInfo._
    import row.gender
    Seq(
      timePoint(
        part.startTime,
        s"${gender.inflect("vyšla", "vyšel")} ${
          previousPartMetaOption.fold("ze startu")(pm =>
            s"z $pos. stanoviště ${pm.place}"
          )
        }"
      ),
    ) ++ (part match {
      case PartTimeInfo.Finished(_startTime, endTime, intervalTime) => Seq(
        process(Seq[Frag](
          strong(s"${partMeta.trackLength}km"),
          s" cesta ${gender.inflect("jí", "mu")} trvala $intervalTime",
          br(),
          "Průměrná rychlost ",
          strong(f"${partMeta.trackLength * 60 / intervalTime.totalMinutes}%1.2f km/h"),
          " = tempo ",
          strong(f"${intervalTime / partMeta.trackLength} / km"),
        )),
        timePoint(
          endTime,
          gender.inflect("dorazila", "dorazil") + s" do ${pos + 1}. stanoviště ${partMeta.place}"
        ),
        nextPartOption.fold(
          if (pos == parts.size-1) finish("cíl")
          else gaveUp(gender.inflect("vzdala", "vzdal") + s" to na $pos. stanovišti")
        ) { nextPart =>
          pause(Seq[Frag](
            "čekání na stanovišti: ",
            strong(s"${TimeInterval((nextPart.startTime - endTime)/60/1000)}")
          ))
        }
      )
      case PartTimeInfo.Unfinished(_startTime) => Seq(
        gaveUp(s"${gender.inflect("Vzdala", "Vzdal")} to při cestě na další stanoviště.")
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
