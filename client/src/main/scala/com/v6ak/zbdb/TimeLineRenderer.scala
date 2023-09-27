package com.v6ak.zbdb

import com.example.RichMoment.toRichMoment
import com.example.moment.{Moment, moment}
import com.v6ak.scalajs.time.TimeInterval
import HtmlUtils._
import TextUtils._
import Bootstrap._
import com.v6ak.zbdb.PartTimeInfo.Finished
import org.scalajs.dom
import scalatags.JsDom.all.{i => iTag, name => _, _}
import scala.scalajs.js
import org.scalajs.dom.raw._


final case class FullPartInfo(
  previousPartMetaOption: Option[Part],
  partMeta: Part,
  part: PartTimeInfo,
  nextPartOption: Option[PartTimeInfo],
)

final class TimeLineRenderer(participantTable: ParticipantTable, plotRenderer: PlotRenderer) {
  import participantTable._
  import plotRenderer.{Plots, initializePlot}

  val switchesState = collection.mutable.Map[String, String](
    "speed" -> "with-speed",
    "time" -> "with-clock-time",
  )

  def bodyClasses = switchesState.values

  val brief = true

  def verbose(f: Frag) = if (brief) "" else f

  def verbose(s: String) = if (brief) "" else s


  final private class ForPlayer(row: Participant) {
    private def timePoint(time: Moment, content: Frag, className: String = "") = tr(
      `class` := s"timeline-point $className",
      td(
        `class` := "timeline-time",
        span(`class` := "clock-time", time.hoursAndMinutes),
        " ",
        span(`class` := "relative-time", "+" + TimeInterval((time - row.startTime) / 60 / 1000)),
      ),
      td(),
      td(`class` := "timeline-content", content),
    )
    private def arrival(time: Moment, content: Frag) = timePoint(time, content, className = "arrival")
    private def departure(time: Moment, content: Frag, start: Boolean) =
      timePoint(time, content, className = if(start) "departure start" else "departure")

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

    private def empty() = tr(
      `class` := s"timeline-process",
      td(`class` := "timeline-time")(br()),
      td(`class` := "timeline-duration"),
      td(`class` := "timeline-content"),
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

    private def classSelect(switchName: String)(items: (String, String)*) = select(
      onchange := { e: Event =>
        val el = e.currentTarget.asInstanceOf[HTMLSelectElement]
        val newClass = el.value
        val oldClasses = items.map(_._1).toSet - newClass
        val classList = dom.document.body.classList
        classList.add(newClass)
        oldClasses.foreach(classList.remove)
        switchesState(switchName) = newClass
      }
    )(
      for ( (cls, name) <- items) yield
        option(value := cls, if(cls == switchesState(switchName)) selected := true else "")(name)
    )

    private def csNumInflection[T](num: Int)(one: T, few: T, many: T) = num match {
      case 1 => one
      case n if n < 5 => few
      case _ => many
    }
    private def peopleList(symbol: String, others: Seq[Participant]) = s"$symbol${others.size} ${csNumInflection(others.size)("človek", "lidi", "lidí")}"
    private def togetherWith(others: Seq[Participant]) = peopleList("+", others)
    private def outran(others: Seq[Participant]) = peopleList("+", others)
    private def wasOutran(others: Seq[Participant]) = peopleList("-", others)

    def timeLine = {
      val prevParts = Seq(None) ++ parts.map(Some(_))
      val nextPartInfos: Seq[Option[PartTimeInfo]] = row.partTimes.drop(1).map(Some(_)) ++ Seq(None)
      div(
        div(`class` := "legend")(
          h2("Legenda"),
          legendTable,
        ),
        div(`class` := "timeline-switches")(
          classSelect("time")(
            "with-relative-time" -> "Čas od startu",
            "with-clock-time" -> "Skutečný čas",
          ),
          classSelect("speed")(
            "with-speed" -> "rychlost (km/h)",
            "with-pace" -> "tempo (mm:ss / km)",
          )
        ),
        table(
          `class` := "timeline timeline-real",
          (
            prevParts lazyZip
              parts lazyZip
              row.partTimes lazyZip
              nextPartInfos
          )
            .map(FullPartInfo)
            .zipWithIndex
            .flatMap((partTimeLine _).tupled)
        ),
        h2("Vizualizace"),
        chartButtons(row),
      )
    }

    private def legendTable = {
      table(
        `class` := "timeline timeline-legend",
        departure(moment("2016-01-20 10:55"), "odchod v 10:55", start = false),
        walk("chůze trvající 4:20", TimeInterval(260)),
        arrival(moment("2016-01-20 15:15"), "příchod v 15:15"),
        pause("pauza trvající 10 minut", TimeInterval(10)),
        gaveUp("konec před dosažením cíle"),
        finish(moment("2016-01-20 16:20"), "Dosažení cíle v 16:20"),
      )
    }

    private def partTimeLine(
      fullPartInfo: FullPartInfo,
      pos: Int,
    ): Seq[Frag] = {
      import fullPartInfo._
      import row.gender
      def langGaveUp = gender.inflect("vzdala", "vzdal")

      def langArrived = gender.inflect("dorazila", "dorazil")

      def cumLen: Frag = fseq(" (celkem ", strong(formatLength(partMeta.cumulativeTrackLength)), ")")

      Seq(
        departure(
          part.startTime,
          fseq(
            previousPartMetaOption.fold[Frag](fseq("Start", br()))(_ => ""),
            togetherWith(filterOthers(pos, row)((me, other) => me.startTime isSame other.startTime))
          ),
          start = previousPartMetaOption.isEmpty
        ),
      ) ++ (part match {
        case PartTimeInfo.Finished(_startTime, endTime, intervalTime) =>
          val isFinish = pos == parts.size - 1
          //noinspection ConvertibleToMethodValue
          val arrivalWith = togetherWith(filterOthers(pos, row)((me, other) =>
            other.endTimeOption.exists(endTime isSame _)
          ))
          fseq(
            walk(
              fseq(
                strong(formatLength(partMeta.trackLength)),
                span(`class` := "speed")(strong(formatSpeed(partMeta.trackLength * 60 / intervalTime.totalMinutes))),
                span(`class` := "pace")(strong(f"${intervalTime / partMeta.trackLength} / km")),
                outran(filterOthers(pos, row)((me, other) => me outran other)),
                wasOutran(filterOthers(pos, row)((me, other) => other outran me)),
              ).map(fseq(_, " ")),
              duration = intervalTime,
            ),
            if (isFinish)
              fseq(
                empty(),
                finish(
                  endTime,
                  fseq(s"Cíl: ${partMeta.place}", cumLen, arrivalWith)
                  // TODO: stats
                )
              )
            else
              arrival(
                endTime,
                arrivalWith,
              ),
            nextPartOption.fold[Frag](
              if (isFinish) ""
              else gaveUp(fseq(s"$langGaveUp to na stanovišti ", strong(s"${pos + 1}."), s" ${partMeta.place}", cumLen))
            ) { nextPart =>
              pause(
                fseq(strong(s"${pos + 1}."), s" ${partMeta.place}", cumLen),
                duration = TimeInterval((nextPart.startTime - endTime) / 60 / 1000)
              )
            }
          )
        case PartTimeInfo.Unfinished(_startTime) => Seq(
          gaveUp(s"$langGaveUp to při cestě na ${pos + 1}. stanoviště.")
        )
      })
    }

  }

  def timeLine(row: Participant) = new ForPlayer(row).timeLine

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
