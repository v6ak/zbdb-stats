package com.v6ak.scalajs.time

case class Pace(secondsPerKm: Int) {
  def minPart: Int = secondsPerKm / 60

  def secPart: Int = secondsPerKm % 60

  override def toString: String = f"$minPart:$secPart%02d"
}
