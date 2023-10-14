package com.v6ak.zbdb

import com.example.RichMoment.toRichMoment
import com.example.moment.{Moment, moment}
import com.v6ak.scalajs.time.TimeInterval
import HtmlUtils._
import TextUtils._
import Bootstrap._
import com.v6ak.zbdb.PartTimeInfo.Finished
import org.scalajs.dom
import scalatags.JsDom._
import scalatags.JsDom.all._
import scala.scalajs.js
import org.scalajs.dom._
import org.scalajs.dom.html.TableRow
import scala.annotation.tailrec

import scala.scalajs.js.annotation._
import ChartJsUtils._


final class TimeLineRenderer(participantTable: ParticipantTable, plotRenderer: PlotRenderer) {
  import participantTable._
  import plotRenderer.IndividualPlots

  private val switches = new ClassSwitches(Map(
    "speed" -> "with-speed",
    "time" -> "with-clock-time",
    "overtaking"-> "without-overtaking",
  ))

  def bodyClasses = switches.values

  final private class ForPlayer(row: Participant) {
    import row.gender
    private def timePoint(time: Moment)(content: Frag*): TypedTag[TableRow] = tr(
      `class` := s"timeline-point",
      td(
        `class` := "timeline-time",
        span(`class` := "clock-time", time.hoursAndMinutes),
        " ",
        span(`class` := "relative-time", "+" + TimeInterval((time - row.startTime) / 60 / 1000)),
      ),
      td(),
      td(`class` := "timeline-content", content),
    )

    @tailrec def findParent(name: String, el: Element): Element =
      if (el.nodeName.toUpperCase == name.toUpperCase()) el
      else findParent(name, el.parentNode.asInstanceOf[Element])

    private def process(content: Frag, duration: TimeInterval, durationIcon: Glyph, className: String = "") = tr(
      `class` := s"timeline-process $className",
      onmouseover := { (e: Event) =>
        val tr = findParent("tr", e.target.asInstanceOf[Element])
        tr.previousSibling.asInstanceOf[Element].classList.add("before-hover")
      },
      onmouseout := { (e: Event) =>
        val tr = findParent("tr", e.target.asInstanceOf[Element])
        tr.previousSibling.asInstanceOf[Element].classList.remove("before-hover")
      },
    )(
      td(`class` := "timeline-time"),
      td(`class` := "timeline-duration")(
        fseq(
          durationIcon.toHtml,
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
      process(content, duration = duration, durationIcon = Glyphs.Pause, className = "pause")

    private def walk(content: Frag, duration: TimeInterval) =
      process(content, duration = duration, durationIcon = Glyphs.Play, className = "walk")

    private def gaveUp(content: Frag) = end(content, className = "gave-up")

    private def csNumInflection[T](num: Int)(one: T, few: T, many: T) = num match {
      case 1 => one
      case n if n < 5 => few
      case _ => many
    }
    private def peopleList(symbol: String, title: String, others: Seq[Participant]): Frag =
      peopleList((symbol, title, others))
    private def peopleList(items: (String, String, Seq[Participant])*): Frag =
      div(cls := "dropdown people-list-dropdown")(
        btnLight(`class`:="btn-sm people-list-expand dropdown-toggle", toggle := "dropdown")(
          items.map{case (symbol, _, others) => s"$symbol${others.size}"}.mkString(""),
          span(cls:="caret"),
        ),
        div(cls := "dropdown-menu")(
          for((_, title, people) <- items) yield fseq(
            h2(title, s" (${people.size})"),
            ul(
              for(person <- people) yield li(s"${person.id}: ${person.fullNameWithNick}")
            )
          )
        )
      )
    private def renderTogetherWith(others: Seq[Participant]) =
      peopleList("=", "Spolu s", others)
    private def renderOvertakes(overtakes: Overtakes): Frag = renderOvertakes(
      overtook = overtakes.overtook,
      overtakenBy = overtakes.overtakenBy
    )
    private def renderOvertakes(
      overtook: Seq[Participant],
      overtakenBy: Seq[Participant],
    ): Frag = fseq(
      peopleList(
        ("+", gender.inflect("Předběhla", "Předběhl"), overtook),
        ("-", gender.inflect("Byla předběhnuta", "Byl předběhnut"), overtakenBy),
      ),
    )

    def timeLine = {
      val events = walkEvents(row)
      val (overtookTotal, overtakenTotal) = events.collect { case p: WalkEvent.Process => p.overtakes }
        .foldLeft((0, 0))((cum, overtakes) =>
          (cum._1 + overtakes.overtook.size, cum._2 + overtakes.overtakenBy.size)
        )
      val totalWalkTime = events
        .collect { case p: WalkEvent.Walk => p.duration }
        .foldLeft(TimeInterval(0))(_ + _)
      val allPauses = events.collect { case p: WalkEvent.WaitingOnCheckpoint => p.duration }
      val cumLenOption = events.collect { case p: WalkEvent.Arrival => p.checkpoint.cumLen }.lastOption
      val totalPausesTime = allPauses.foldLeft(TimeInterval(0))(_ + _)
      val totalTime = totalWalkTime + totalPausesTime
      val idPrefix = IdGenerator.newId()
      val out = div(
        intro,
        table(`class` := "timeline timeline-real")(events.map(renderWalkEvent)),
        h2("Celkový čas a rychlost"),
        s"Pochod ${gender.inflect("jí", "mu")} trval celkem ",
        strong(totalTime.toString),
        s", z toho $totalWalkTime čistá chůze a $totalPausesTime čekání na stanovištích.",
        if(allPauses.nonEmpty) s" Na jednom stanovišti čekal$langGenderSuffix průměrně ${totalPausesTime / allPauses.size}."
        else "",
        cumLenOption.fold("":Frag)(cumLen => p(
          "Na ", strong(formatLength(cumLen)), s" cestě ${gender.inflect("šla", "šel")} průměrně ",
          renderSpeedAndPace(totalTime, cumLen), " včetně pauz, resp. ", renderSpeedAndPace(totalWalkTime, cumLen),
          " čisté chůze.",
        )),
        h2("Předbíhání"),
        p("Za jeden úsek cesty počítáme předběhnutí maximálně jednou. Počítáme předběhnutí i na stanovišti."),
        ul(
          li(s"Předběhl$langGenderSuffix $overtookTotal×"),
          li(s"Byl$langGenderSuffix předběhnut$langGenderSuffix $overtakenTotal×"),
        ),
        switches.checkbox(
          "overtaking", "Zobrazit předbíhání v průběhu pochodu"
        )(
          "with-overtaking", "without-overtaking"
        ),
        for((plot, i) <- IndividualPlots.zipWithIndex) yield fseq(
          h2(s"Graf ${plot.nameGenitive}"),
          div(id := s"$idPrefix-$i")
        ),
      ).render
      (out, createRenderPlots(idPrefix))
    }

    private def createRenderPlots(idPrefix: String) =
      (dialog: HTMLElement) => {
        for ((plot, i) <- IndividualPlots.zipWithIndex) {
          val data = plot.generator(Seq(row)).asInstanceOf[js.Dynamic]
          val id = s"$idPrefix-$i"
          val element = document.getElementById(id).asInstanceOf[HTMLElement]
          if (data.data.datasets.asInstanceOf[js.Array[js.Dynamic]](0).data.length == 0.asInstanceOf[js.Dynamic]) {
            element.appendChild("Žádná data".render)
          } else {
            initializePlot(element, data, cb => dialog.onBsModalHidden(cb))
          }
        }
      }

    private def intro = {
      fseq(
        div(`class` := "legend")(
          h2("Legenda"),
          legendTable,
        ),
        div(`class` := "timeline-switches")(
          switches.classSelect("time")(
            "with-relative-time" -> "Čas od startu",
            "with-clock-time" -> "Skutečný čas",
          ),
          switches.classSelect("speed")(
            "with-speed" -> "rychlost (km/h)",
            "with-pace" -> "tempo (mm:ss / km)",
          )
        ),
      )
    }

    private def langGenderSuffix = {
      gender.inflect("a", "")
    }

    private def legendTable = {
      table(
        `class` := "timeline timeline-legend",
        timePoint(moment("2016-01-20 10:55"))("odchod v 10:55")(`class` := "departure"),
        walk("chůze trvající 4:20", TimeInterval(260)),
        timePoint(moment("2016-01-20 15:15"))("příchod v 15:15")(`class` := "arrival"),
        pause("pauza trvající 10 minut", TimeInterval(10)),
        gaveUp("konec před dosažením cíle"),
        timePoint(moment("2016-01-20 16:20"))("Dosažení cíle v 16:20")(`class` := "finish"),
      )
    }

    private def renderCumLen(cumLen: BigDecimal): Frag = fseq(
      " (celkem ", strong(formatLength(cumLen)), ")"
    )

    private def renderCheckpoint(checkpoint: Checkpoint) = emptyCheckpoint(
      fseq(
        s"${checkpoint.pos + 1}",
        span(`class` := "dot", aria.hidden := "false")("."),
      )
    )

    private def langGaveUp = gender.inflect("vzdala", "vzdal")

    def renderWalkEvent(walkEvent: WalkEvent): Frag = walkEvent match {
      case tp: WalkEvent.TimePoint =>
        timePoint(tp.time)(
          renderTogetherWith(tp.togetherWith),
          if (tp.isStart) fseq(emptyCheckpoint, " Start") else fseq(),
          if (tp.isFinish) fseq(emptyCheckpoint, s" Cíl: ${tp.checkpoint.place}", renderCumLen(tp.checkpoint.cumLen))
          else fseq(),
        )(
          `class` := (tp match {
            case _: WalkEvent.Departure => "departure"
            case _: WalkEvent.Arrival => "arrival"
          }),
          if (tp.isStart) `class` := "start" else "",
          if (tp.isFinish) `class` := "finish" else "",
        )
      case proc: WalkEvent.Process => proc match {
        case WalkEvent.Walk(duration, overtakes, len) => walk(
          spaceSeparated(
            renderOvertakes(overtakes),
            strong(formatLength(len)),
            renderSpeedAndPace(duration, len),
          ),
          duration = duration,
        )
        case WalkEvent.WaitingOnCheckpoint(checkpoint, duration, overtakes) =>
          pause(
            fseq(
              renderOvertakes(overtakes),
              renderCheckpoint(checkpoint),
              s" ${checkpoint.place}",
              renderCumLen(checkpoint.cumLen),
            ),
            duration = duration,
          )
      }
      case utp: WalkEvent.UnknownTimePoint => utp match {
        case WalkEvent.GaveUp.DuringWalk(nextPos: Int) =>
          gaveUp(s"$langGaveUp to při cestě na ${nextPos}. stanoviště.")
        case WalkEvent.GaveUp.AtCheckpoint(checkpoint) =>
          gaveUp(
            fseq(
              s"$langGaveUp to na stanovišti ",
              renderCheckpoint(checkpoint),
              s" ${checkpoint.place}",
              renderCumLen(checkpoint.cumLen),
            )
          )
      }
    }

    private def emptyCheckpoint = {
      span(`class` := "checkpoint")
    }
  }

  private def renderSpeedAndPace(duration: TimeInterval, len: BigDecimal) = fseq(
    span(`class` := "speed")(strong(formatSpeed(len * 60 / duration.totalMinutes))),
    " ",
    span(`class` := "pace")(strong(f"${duration / len} / km"))
  )

  def timeLine(row: Participant) = new ForPlayer(row).timeLine

}
