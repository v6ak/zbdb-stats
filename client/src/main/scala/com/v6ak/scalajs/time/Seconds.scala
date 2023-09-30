package com.v6ak.scalajs.time

case class Seconds(totalSeconds: Int) extends AnyVal {
  def hours: Int = totalSeconds / 3600

  def minutes: Int = (totalSeconds / 60) % 60

  def seconds: Int = totalSeconds % 60

  override def toString: String = f"$hours:$minutes:$seconds"
}
