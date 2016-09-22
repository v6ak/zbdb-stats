package com.v6ak.zbdb

import com.v6ak.zbdb.PartTimeInfo.Finished
import org.scalajs.dom

final case class Participant(
  id: Int,
  firstName: String,
  lastName: String,
  nickOption: Option[String],
  gender: Gender,
  //age: String,
  last3: Seq[String], // TODO: parse
  partTimes: Seq[PartTimeInfo]
) {
  def startTime = partTimes.head.startTime

  def finishedPartTimes = partTimes.takeWhile(_.isInstanceOf[Finished]).map(_.asInstanceOf[Finished])

  def fullNameWithNick: String = fullName+nickOption.fold("")(" '"+_+"'")

  def fullName: String = firstName+" "+lastName

  def pauses: Seq[Int] = {
    dom.console.log("pauses: partTimes.size", partTimes.size)
    dom.console.log("pauses: partTimes.sliding(2).size", partTimes.sliding(2).size)
    partTimes.sliding(2).map{case Seq(previous, next) => next.startTime - previous.endTimeOption.get}.toSeq
  }

}
