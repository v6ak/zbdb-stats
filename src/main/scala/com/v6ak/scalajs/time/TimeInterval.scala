package com.v6ak.scalajs.time

case class TimeInterval(totalMinutes: Int) extends AnyVal{
  def hours = totalMinutes/60
  def minutes = totalMinutes%60
  override def toString: String = f"$hours:$minutes%02d"
}

object TimeInterval{
  private val TimeIntervalRegex = """^([0-9]+):([0-9]+)$""".r

  def parse(s: String) = s match {
    case TimeIntervalRegex(hs, ms) => TimeInterval(hs.toInt*60 + ms.toInt)
  }

}