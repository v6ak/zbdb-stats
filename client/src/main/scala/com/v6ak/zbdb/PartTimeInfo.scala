package com.v6ak.zbdb

import com.example.moment.Moment
import com.v6ak.scalajs.time.TimeInterval

abstract sealed class PartTimeInfo{
  def endTimeOption: Option[Moment]
  def startTime: Moment
  def durationOption = endTimeOption.map(_ - startTime)
  def lastTime: Moment
}

object PartTimeInfo{
  final case class Finished(startTime: Moment, endTime: Moment, intervalTime: TimeInterval) extends PartTimeInfo {
    def outran(other: Finished) = this.startTime >= other.startTime && this.endTime < other.endTime

    def crosses(other: Finished) = ((this.startTime >= other.startTime) && (this.endTime <= other.endTime)) || ((this.startTime <= other.startTime) && (this.endTime >= other.endTime))

    override def endTimeOption: Option[Moment] = Some(endTime)

    override def lastTime: Moment = endTime
  }
  final case class Unfinished(startTime: Moment) extends PartTimeInfo {
    override def endTimeOption: Option[Moment] = None

    override def lastTime: Moment = startTime
  }
}