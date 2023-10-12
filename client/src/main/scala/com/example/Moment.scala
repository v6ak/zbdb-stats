package com.example

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.language.implicitConversions

object `package` {
  type Moment = ZonedDateTime
}

package object moment {
  type Moment = ZonedDateTime
  object moment {
    @inline def apply(moment: Moment) = moment // we don't need to clone immutable value
    @deprecated @inline def apply(moment: String) = ZonedDateTime.parse(moment) // we don't need to clone immutable value
    def tz(time: String, tz: String): Moment = ZonedDateTime.of(LocalDateTime.parse(time), ZoneId.of(tz))
  }
}

package moment{



}

class RichMoment(val moment: Moment) extends AnyVal{

  def >=(other: Moment):Boolean = moment.isEqual(other) || (moment isAfter other)
  def <=(other: Moment):Boolean = moment.isEqual(other) || (moment isBefore other)
  @inline def >(other: Moment): Boolean = moment isAfter other
  @inline def <(other: Moment): Boolean = moment isBefore other
  def isSame(other: Moment): Boolean = moment isEqual other
  def -(other: Moment): Int = ???

  def hoursAndMinutes = f"${moment.getHour}%d:${moment.getMinute}%02d"

}

object RichMoment{
  implicit def toRichMoment(moment: Moment): RichMoment = new RichMoment(moment)
}
