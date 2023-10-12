package com.v6ak.zbdb

import com.v6ak.scalajs.time.TimeInterval
import com.v6ak.zbdb.PartTimeInfo.Finished
import org.scalajs.dom
import com.example.RichMoment.toRichMoment

final case class Participant(
  id: Int,
  firstName: String,
  lastName: String,
  nickOption: Option[String],
  gender: Gender,
  age: String,
  birthYear: Option[Int],
  last3: Seq[String], // TODO: parse
  partTimes: Seq[PartTimeInfo]
) {

  def totalTime: TimeInterval = partTimes.headOption.fold(TimeInterval(0))(head=>
    TimeInterval.fromMilliseconds(partTimes.last.lastTime - head.startTime)
  )

  def startTime = partTimes.head.startTime

  def finishedPartTimes = partTimes.takeWhile(_.isInstanceOf[Finished]).map(_.asInstanceOf[Finished])

  def fullNameWithNick: String = fullName+nickOption.fold("")(" '"+_+"'")

  def fullName: String = firstName+" "+lastName

  def pauses: Seq[Pause] =
    partTimes match {
      case Seq() | Seq(_) => Seq()
      case partTimesWithPauses => partTimesWithPauses.sliding(2).map { case Seq(previous, next) =>
        Pause(
          startTime = previous.endTimeOption.get,
          endTime = next.startTime
        )
      }.toSeq
    }

  def pauseTimes = pauses.map(p => p.endTime - p.startTime)

}
