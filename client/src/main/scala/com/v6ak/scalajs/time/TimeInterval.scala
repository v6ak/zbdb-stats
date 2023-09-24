package com.v6ak.scalajs.time

import com.v6ak.scalajs.regex.JsPattern.wrapString

import java.math.MathContext

case class TimeInterval(totalMinutes: Int) extends AnyVal{
  def /(trackLength: BigDecimal): Pace = {
    val paceSecPerKm = totalMinutes * 60.0 / trackLength
    Pace(paceSecPerKm.round(MathContext.UNLIMITED).toInt)
  }

  def hours = totalMinutes/60
  def minutes = totalMinutes%60
  def -(other: TimeInterval) = TimeInterval(this.totalMinutes - other.totalMinutes)
  override def toString: String = f"$hours:$minutes%02d"
}

object TimeInterval{
  private val TimeIntervalRegex = """^([0-9]+):([0-9]+)$""".jsr

  def parse(s: String) = s match {
    case TimeIntervalRegex(hs, ms) => TimeInterval(hs.toInt*60 + ms.toInt)
  }

  def fromMilliseconds(millis: Long) = {
    val minutes = (millis / 1000 / 60).toInt
    if(minutes*60*1000 != millis){
      sys.error("precision loss")
    }
    TimeInterval(minutes)
  }

  implicit val TimeIntervalOrdering: Ordering[TimeInterval] = Ordering.by(_.totalMinutes)

}