package com.v6ak.zbdb

import com.example.moment.Moment
import com.v6ak.scalajs.time.TimeInterval
import com.v6ak.zbdb.PartTimeInfo.Finished

object BestParticipantData{
  val Empty = BestParticipantData(None, None, None)
  private def minTimeOption(a: Option[Moment], b: Option[Moment]): Option[Moment] = a match { // minimize garbage
    case None => b // (None, b) --> b
    case Some(firstTime) => b match {
      case None => a // (a@Some(firstTime), None) --> a
      case Some(secondTime) => if(firstTime < secondTime) a else b // (a@Some(firstTime), b@Some(secondTime)) --> compare firstTime and secondTime
    }
  }
  private def minDurationOption(a: Option[Int], b: Option[Int]): Option[Int] = a match { // minimize garbage
    case None => b // (None, b) --> b
    case Some(firstDuration) => b match {
      case None => a // (a@Some(firstDuration), None) --> a
      case Some(secondDuration) => if(firstDuration < secondDuration) a else b // (a@Some(firstDuration), b@Some(secondDuration)) --> compare firstDuration and secondDuration
    }
  }
}


final case class BestParticipantData(private val endTimeOption: Option[Moment], durationOption: Option[Int], private val startTimeOption: Option[Moment]) {
  def hasBestDuration(pti: Finished): Boolean = pti.durationOption == this.durationOption
  def hasBestEndTime(partTimeInfoOption: PartTimeInfo): Boolean = partTimeInfoOption.endTimeOption.map(_.unix()) == endTimeOption.map(_.unix())
  def hasBestStartTime(partTimeInfoOption: PartTimeInfo): Boolean = startTimeOption.exists(bestStartTime => bestStartTime.unix() == partTimeInfoOption.startTime.unix())

  import BestParticipantData._

  def merge(current: Option[PartTimeInfo]): BestParticipantData = current match {
    case None => this
    case Some(partTimeInfo) =>
      BestParticipantData(
        endTimeOption = minTimeOption(partTimeInfo.endTimeOption, endTimeOption),
        startTimeOption = minTimeOption(Some(partTimeInfo.startTime), startTimeOption),
        durationOption = minDurationOption(partTimeInfo.durationOption, durationOption)
      )
  }

}
