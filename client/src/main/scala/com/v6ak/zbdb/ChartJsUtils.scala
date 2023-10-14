package com.v6ak.zbdb

import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom._
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.JSConverters._
import scalatags.JsDom.all._
import Bootstrap.DialogUtils
import com.example.moment.{Moment, moment}

@JSGlobal @js.native class Chart(el: Element, data: js.Any) extends js.Object {
  def resize(): Unit = js.native
  def update(): Unit = js.native
  def destroy(): Unit = js.native
}

object ChartJsUtils {
  // Chart.register(…)

  def zeroMoment = moment("2000-01-01") // We don't want to mutate it

  def timeAxis(label: String, min: js.Any = js.undefined) = literal(
    `type` = "time",
    min = min,
    time = literal(
      unit = "minute",
      displayFormats = literal(
        minute = "HH:mm",
      ),
    ),
    ticks = literal(
      stepSize = 5,
    ),
    title = literal(
      display = true,
      text = label,
    ),
  )

  def durationAxis(label: String, min: js.Any) = literal(
    `type` = "time",
    min = min,
    ticks = literal(
      callback = (value: Moment, _index: js.Dynamic, _ticks: js.Dynamic) => {
        val diff = value - zeroMoment
        val hours = diff / 1000 / 60 / 60
        val minutes = diff / 1000 / 60 - hours * 60
        f"$hours%02d:$minutes%02d"
      },
      stepSize = 30,
    ),
    title = literal(
      display = true,
      text = label,
    ),
  )
  def minutesAxis(label: String) = literal(
    min = 0,
    title = literal(
      display = true,
      text = label,
    ),
  )

  def distanceAxis = {
    literal(
      `type` = "linear",
      min = 0,
      title = literal(
        display = true,
        text = "vzdálenost (km)",
      ),
    )
  }

  def speedAxis = {
    literal(
      `type` = "linear",
      min = 0,
      title = literal(
        display = true,
        text = "rychlost (km/h)",
      ),
    )
  }

  def dataFromPairs(seq: Seq[(Any, Any)]) = literal(
    labels = seq.map(_._1).toJSArray,
    datasets = js.Array(
      literal(
        data = seq.map(_._2).toJSArray,
      )
    )
  )

  def dataFromTriples(seq: Seq[(Any, Any, String)]) = literal(
    labels = seq.map(_._1).toJSArray,
    datasets = js.Array(
      literal(
        backgroundColor = seq.map(_._3).toJSArray,
        data = seq.map(_._2).toJSArray,
      )
    )
  )

  def initializePlot(plotRoot: HTMLElement, plotParams: js.Any, registerDestroy: (()=>Unit) => Unit): Unit = {
    console.log("plotParams", plotParams)

    val can = canvas().render
    plotRoot.appendChild(
      div(`class` := s"ratio plot plot-${plotParams.asInstanceOf[js.Dynamic].`type`}")(
        div(can)
      ).render
    )

    val chart = new Chart(can, plotParams)
    val resizeHandler: Event => Unit = _ => {
      //chart.update()
      chart.resize()
    }

    window.addEventListener("resize", resizeHandler)

    def destroy(): Unit = {
      window.removeEventListener("resize", resizeHandler)
      chart.destroy()
    }

    registerDestroy(destroy)
  }

  def showChartInModal(title: String = null)(f: Seq[Participant] => js.Any): (Option[String], (HTMLElement, => Seq[Participant], ParticipantTable) => Unit) =
    Option(title) -> ((el, rowsLoader, participantTable: Any) => {
      val plotParams = f(rowsLoader)
      val modalElement = el.parentNode.parentNode.parentNode.asInstanceOf[Element]
      initializePlot(el, plotParams, destroy => modalElement.onBsModalHidden(destroy))
    }
  )

  def plotLinesToData(data: Seq[PlotLine]) = {
    literal(
      datasets = data.map(ser =>
        literal(
          label = s"${ser.row.id}: ${ser.row.fullNameWithNick}",
          data = ser.points,
        )
      ).toJSArray,
    )
  }

}
