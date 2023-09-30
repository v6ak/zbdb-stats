package com.v6ak.zbdb

import com.example.moment.Moment
import com.example.RichMoment._

final case class Pause(startTime: Moment, endTime: Moment) {
  def overtook(other: Pause): Boolean = this.startTime >= other.startTime && this.endTime < other.endTime
}
