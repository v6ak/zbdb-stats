package com.v6ak.zbdb

import com.example.RichMoment.*
import com.example.moment.*
import com.v6ak.zbdb.ChartJsUtils.*
import com.v6ak.zbdb.CollectionUtils.RichMap
import com.v6ak.zbdb.RichGenderSeq.*
import com.v6ak.zbdb.TextUtils.{formatLength, formatSpeed}
import org.scalajs.dom
import org.scalajs.dom.*

import scala.collection.immutable
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.JSConverters.*


final class PlotRenderer(participantTable: ParticipantTable):

  import participantTable.*

  val IndividualPlots = Seq(
    ParticipantPlotGenerator("chůze", "chůzi", Glyphs.Pedestrian, generateWalkPlotData),
    ParticipantPlotGenerator("rychlosti", "rychlost", Glyphs.Play, generateSpeedPlotData),
    ParticipantPlotGenerator("pauz", "pauzy", Glyphs.Pause, generatePausesPlotData)
  )

  val GlobalPlots = Seq[(String, (Option[String], ((HTMLElement, =>Seq[Participant], ParticipantTable) => Unit)))](
    "Porovnání startu a času" -> startTimeToTotalDurationPlot,
    "Genderová struktura" -> genderStructurePlot,
    "Počet lidí" -> remainingParticipantsCountPlot,
    "Počet lidí v %" -> remainingRelativeCountPlot,
  )

  private val GenderNames = Map[Gender, String](
    Gender.Male -> "muž",
    Gender.Female -> "žena"
  )

  private val GenderColors = Map[Gender, String](
    Gender.Female -> "#FFC0CB",
    Gender.Male -> "#008080",
  )

  private val startTimeToTotalDurationRadius = (context: js.Dynamic) =>
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

  private def startTimeToTotalDurationTooltip =
    literal(
      callbacks = literal(
        label = (context: js.Dynamic) => {
          val participants = context.raw.participants.asInstanceOf[js.Array[Participant]]
          val names = participants.map(_.fullName).mkString(", ")
          val started = participants.map(_.gender).inflectCzech(
            feminineSingular = "Vyrazila",
            femininePlural = "Vyrazily",
            masculineSingular = "Vyrazil",
            masculinePlural = "Vyrazili",
          )
          val finished = participants.map(_.gender).inflectCzech(
            feminineSingular = "ušla",
            femininePlural = "ušly",
            masculineSingular = "ušel",
            masculinePlural = "ušli",
          )
          val start = context.raw.x.asInstanceOf[Moment].hoursAndMinutes
          val total = context.raw.y.asInstanceOf[Moment].hoursAndMinutes
          s"$names (${participants.size}) – $started $start, celou trasu $finished za $total"
        },
      ),
    )

  private def startTimeToTotalDurationPlot = showChartInModal(
    title = "Porovnání startu a celkového času (pouze finalisti)"
  ): rows =>
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
          x = timeAxis(
            label = "čas startu",
            min = participantTable.startTime.subtract(5, "minutes").unix().toDouble * 1000,
          ),
          y = durationAxis(
            label="celková doba",
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

  private def computeCumulativeMortality(rows: Seq[Participant]) =
    val mortalityMap = rows.map(_.partTimes.count(_.hasEndTime)).groupBy(identity).mapValuesStrict(_.size)
    val mortalitySeq = (0 to mortalityMap.keys.max).map(mortalityMap.getOrElse(_, 0))
    mortalitySeq.scan(0)(_ + _).tail

  def remainingParticipantsCountPlot = showChartInModal(): rows =>
    val cummulativeMortality: immutable.IndexedSeq[Int] = computeCumulativeMortality(rows)
    val data = cummulativeMortality.dropRight(1).zipWithIndex.map{case (cm, i) =>
      (participantTable.parts(i), cm, rows.size - cm, i)
    }
    genericRemainingParticipantsCountPlot(data, "line", "lidi")(_.toString)

  def remainingRelativeCountPlot = showChartInModal(): rows =>
    val cummulativeMortality: immutable.IndexedSeq[Int] = computeCumulativeMortality(rows)
    val size = rows.size
    val data = cummulativeMortality.dropRight(1).zipWithIndex.map { case (cm, i) =>
      (participantTable.parts(i), 100.0 * cm / size, 100.0 * (size - cm) / size, i)
    }
    genericRemainingParticipantsCountPlot[Double](data, "bar", "lidi (%)")(num => f"$num%.2f%%")

  private def genericRemainingParticipantsCountPlot[T](
    data: immutable.IndexedSeq[(Part, T, T, Int)],
    chartType: String,
    yLabel: String,
  )(format: T => String) =
    def dataset(name: String, f: ((Part, T, T, Int)) => T): js.Dynamic = literal(
      label = name,
      data = data.map { case data@(part, _gaveUp, _remaining, _i) =>
        literal(
          y = f(data).asInstanceOf[js.Any],
          x = part.cumulativeTrackLength.toDouble,
          part = part.asInstanceOf[js.Any],
        )
      }.toJSArray
    )

    literal(
      `type` = chartType,
      data = literal(
        datasets = js.Array(
          dataset("dorazilo", _._3),
          dataset("skončilo", _._2),
        )
      ),
      options = literal(
        plugins = literal(
          tooltip = literal(
            callbacks = literal(
              label = (context: js.Dynamic) => {
                val part = context.raw.part.asInstanceOf[Part]
                val index = context.dataIndex.asInstanceOf[Int] + 1
                val pointName = if(participantTable.parts.size == index) "cíle" else s"$index. stanoviště"
                s"Do $pointName ${context.dataset.label} ${format(context.parsed.y.asInstanceOf[T])} lidí " +
                  s"(${formatLength(part.cumulativeTrackLength)}, ${part.place})"
              },
            )
          )
        ),
        scales = literal(
          x = literal(
            `type` = "linear",
            min = 0,
            title = literal(
              display = true,
              text = "vzdálenost (km)",
            ),
          ),
          y = literal(
            title = literal(
              display = true,
              text = yLabel,
            ),
          )
        ),
      )
    )

  def genderStructurePlot = showChartInModal(
    title = "Genderová struktura startujících",
  ): rows =>
    val structure = rows.groupBy(_.gender)
    js.Dynamic.literal(
      `type` = "pie",
      data = dataFromTriples(structure.toSeq.map { case (gender, p) =>
        (GenderNames(gender), p.size, GenderColors(gender))
      }),
    )

  def expectOneStr(contexts: js.Array[js.Dynamic])(f: js.Dynamic => js.Any): String =
    contexts.map(f).toSet.mkString("  |  ")
  def expectOne[F, T](contexts: js.Array[F])(f: F => T): T = contexts.map(f).toSet.toSeq match
    case Seq(x) => x
    case other => sys.error(s"Expected exactly one value, got: $other")

  private def generateWalkPlotData(rowsLoader: Seq[Participant]) =
    val data = rowsLoader.map(processTimes)
    literal(
      `type` = "line",
      data = plotLinesToData(data),
      options = literal(
        scales = literal(
          x = timeAxis("čas"),
          y = distanceAxis,
        ),
        plugins = literal(
          tooltip = literal(
            callbacks = literal(
              label = (context: js.Dynamic) => context.dataset.label,
              title = (context: js.Array[js.Dynamic]) => {
                val len = expectOneStr(context)(c => formatLength(c.raw.y.asInstanceOf[BigDecimal]))
                val t = expectOneStr(context)(_.raw.x.asInstanceOf[Moment].hoursAndMinutes)
                s"$len v $t"
              },
            ),
          ),
        ),
      ),
    )

  private def generateSpeedPlotData(rows: Seq[Participant]) =
    val data = rows.map{p =>
      PlotLine(row = p, label = p.fullName, points =
        (p.partTimes lazyZip parts).flatMap((partTime, part) => partTime.durationOption.map { duration =>
          literal(
            x = part.cumulativeTrackLength.toDouble,
            y = part.trackLength.toDouble / (duration.toDouble / 1000 / 3600),
          ): js.Any
        }).toJSArray
      )
    }
    literal(
      `type` = "bar",
      data = plotLinesToData(data),
      options = literal(
        scales = literal(
          x = distanceAxis,
          y = speedAxis,
        ),
        plugins = literal(
          tooltip = literal(
            callbacks = literal(
              label = (context: js.Dynamic) =>
                s"${formatSpeed(context.raw.y.asInstanceOf[Double])} (${context.dataset.label})",
              title = (context: js.Array[js.Dynamic]) => {
                val i = expectOne(context)(_.dataIndex.asInstanceOf[Int])
                val from = i match {
                  case 0 => "Ze startu"
                  case i => s"Z $i. stanoviště (${participantTable.parts(i-1).place})"
                }
                val finishCheckpoint = participantTable.parts.size
                val to = i+1 match {
                  case next if next == finishCheckpoint => "do cíle"
                  case next => s"na $next. stanoviště (${participantTable.parts(next-1).place})"
                }
                s"$from $to"
              },
            ),
          ),
        ),
      ),
    )

  private def generatePausesPlotData(rows: Seq[Participant]) =
    val data = rows.map{p =>
      PlotLine(
        row = p,
        label = p.fullName,
        points = (p.pauseTimes lazyZip parts).map((pause, part) =>
          literal(x=part.cumulativeTrackLength.toDouble, y=pause/1000/60): js.Any
        ).toJSArray
      )
    }
    literal(
      `type` = "bar",
      data = plotLinesToData(data),
      options = literal(
        scales = literal(
          x = distanceAxis,
          y = minutesAxis("trvání pauzy (min)"),
        ),
        plugins = literal(
          tooltip = literal(
            callbacks = literal(
              label = (context: js.Dynamic) =>
                s"${context.raw.y.asInstanceOf[Int]} min (${context.dataset.label})",
              title = (context: js.Array[js.Dynamic]) => {
                val i = expectOne(context)(_.dataIndex.asInstanceOf[Int])
                s"${i+1}. stanoviště (${participantTable.parts(i).place})"
              },
            ),
          ),
        )
      )
    )

  private def processTimes(participant: Participant): PlotLine =
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
      points = data.map[js.Any]{case (x, y) => literal(x=x, y=y.asInstanceOf[js.Any])}.toJSArray
    )
