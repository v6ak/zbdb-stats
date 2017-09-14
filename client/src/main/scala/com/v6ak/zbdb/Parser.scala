package com.v6ak.zbdb

import java.io.StringReader

import com.example.moment._
import com.github.marklister.collections.io.CSVReader
import com.v6ak.scalajs.time.TimeInterval
import com.v6ak.zbdb.PartTimeInfo.Finished
import org.scalajs.dom

import scala.collection.immutable
import scala.scalajs.js
import scala.util.Try
import scala.util.matching.Regex

object Parser{

  import Assertions._

  private val StrictMode = true

  private val TrackLengthRegex = """^([0-9]+(?:,[0-9]+)?)\s?(?:km)?$""".r

  private def parseTrackLength(s: String) = s match {
    case TrackLengthRegex(tl) => BigDecimal(tl.replace(',', '.'))
  }

  private val TimeRegexp = """^([0-9]+):([0-9]+)$""".r

  private def strictCheck(f: => Unit): Unit = {
    if(StrictMode){
      f
    }
  }

  final case class Time(h: Int, m: Int){
    private def timeString = s"$h:$m"
    def toMoment(prevTime: Moment, maxHourDelta: Int) = {
      if(!prevTime.isValid()){
        sys.error("invalid prevTime: "+prevTime)
      }
      val result = moment(prevTime)
      result.hours(h)
      result.minutes(m)
      if(result isBefore prevTime){
        result.date(result.date + 1)
      }
      if(!result.isValid()){
        sys.error(s"invalid date: $timeString")
      }
      strictCheck{
        if((result.toDate().getTime() - prevTime.toDate().getTime()) > (maxHourDelta*3600*1000)){
          throw new MaxHourDeltaExceededException(maxHourDelta, prevTime, result)//) sys.error(s"following time specification overreaches time delta $maxHourDelta hours ($prevTime -> $result): $timeString")
        }
      }
      result
    }

    def toTimeInterval = TimeInterval(h*60 + m)

  }
  object Time{
    def unapply(s: String): Option[Time] = s match{
      case TimeRegexp(hs, ms) => Some(Time(hs.toInt, ms.toInt))
      case _ => None
    }
  }

  private def guard[T](data: Seq[String])(f: => T) = try{
    f
  }catch{
    case e: Throwable => throw CellsParsingException(data, e)
  }

  private val Empty = """^(?:x|X|)$""".r

  private def parseTimeInfo(data: Seq[String], prevTimeOption: Option[Moment], maxHourDelta: Int): Option[PartTimeInfo] = guard(data){
    data match {
      case Seq(Empty(), Empty(), Empty()) => None
      case Seq(Time(startTime), Empty(), Empty()) =>
        val prevTime = prevTimeOption.get
        Some(PartTimeInfo.Unfinished(startTime.toMoment(prevTime, maxHourDelta)))
      case Seq(Time(startTime), Time(interval), Time(endTime)) =>
        val prevMoment = prevTimeOption.get
        val startMoment = startTime.toMoment(prevMoment, maxHourDelta)
        val endMoment = endTime.toMoment(startMoment, maxHourDelta)
        val intervalTime = interval.toTimeInterval
        assert( (endMoment.toDate.getTime - startMoment.toDate.getTime) == (intervalTime.totalMinutes*60*1000) )
        Some(PartTimeInfo.Finished(
          startTime = startMoment,
          endTime = endMoment,
          intervalTime = intervalTime
        ))
      case _=> throw BadTimeInfoFormatException()
    }
  }

  def parse(csvData: String, startTime: Moment, totalEndTime: Moment, maxHourDelta: Int, formatVersion: FormatVersion) = {
    val fullDataTable = new CSVReader(new StringReader(csvData.trim)).toIndexedSeq.map(_.toIndexedSeq)
    val Seq(header1, header2, header3, dataWithTail @ _*) = fullDataTable.drop(formatVersion.headSize)
    val (dataTable, footer) = formatVersion.tail.split(dataWithTail.dropWhile(_.head == "").toIndexedSeq)
    footer.foreach{fl =>
      assertEmpty(fl.toSet.filterNot(_.forall(c => c.isDigit || c==':')) -- Set("", "nejdříve na stanovišti", "nejrychleji projitý úsek", "Na trati"))
    }
    val parts = (header1, header2, header3).zipped.toIndexedSeq.drop(3+formatVersion.nameFormat.size).dropRight(4).grouped(3).map{ case Seq((""|"čas startu", "", "odch"), (" =>"|"=>", trackLengthString, ""), (place, cumulativeTrackLengthString, "přích")) =>
      Part(
        place = place,
        trackLength = parseTrackLength(trackLengthString),
        cumulativeTrackLength = parseTrackLength(cumulativeTrackLengthString)
      )
    }.toIndexedSeq
    val parsedDataTries = dataTable.filter(row => row.tail.exists(isUsefulCell)).map{ participantData =>
      Try(
        Left(parseParticipant(participantData, parts, startTime, maxHourDelta, totalEndTime = totalEndTime, formatVersion = formatVersion))
      ).recover{
        case e: Throwable => Right(participantData, e)
      }.get
    }
    val (parsedDataSuccessfulTries, parsedDataFailedTries) = parsedDataTries.partition(_.isLeft)
    if(false && parsedDataFailedTries.nonEmpty){
      dom.console.error(s"parsing some data failed (${parsedDataFailedTries.size}):")
      parsedDataFailedTries foreach { case Right((data, e)) =>
        dom.console.error("following row is not successfuly parsed:", js.Array(data : _*))
        e.printStackTrace()
      }
    }
    (parts, parsedDataSuccessfulTries.map{case Left(p) => p}, parsedDataFailedTries.map{case Right((row, e)) => (row, e)})
  }

  private def isUsefulCell(cell: String): Boolean = cell != "" && cell != "X" && cell != "0:00"

  private def parseParticipant(participantData: immutable.IndexedSeq[String], parts: immutable.IndexedSeq[Part], startTime: Moment, maxHourDelta: Int, totalEndTime: Moment, formatVersion: FormatVersion): Participant = {
    val Seq(num, participantDataAfterNum@_*) = participantData
    val (firstName, lastName, nick, participantDataAfterName) = formatVersion.nameFormat.parse(participantDataAfterNum)
    val Seq(genderString, ageString, other@_*) = participantDataAfterName
    if ((formatVersion.ageType == AgeType.No) && (ageString != "")) {
      sys.error("You seem to have left some data in age column. Clean it first, please.")
    }
    val timesUnparsed = other.dropRight(4).grouped(3).toIndexedSeq
    val last3 = other.takeRight(4)
    //dom.console.log("unparsedTimes", timesUnparsed.toString, timesUnparsed.size)
    val timeOptions = timesUnparsed.tail.scanLeft(parseTimeInfo(timesUnparsed.head, Some(startTime), maxHourDelta)) { (previous, data) =>
      parseTimeInfo(data, previous.collect { case i: Finished => i.endTime }, maxHourDelta)
    }
    strictCheck{
      if(timeOptions.exists(_.exists{ti => (ti.startTime isAfter totalEndTime) || ti.endTimeOption.exists(endTime => endTime isAfter totalEndTime)})){
        throw new DeadlineExceededException()
      }
    }
    assertEquals(parts.size, timeOptions.size)
    val (timeSomes, supposedlyEmptys) = timeOptions.span(_.isDefined)
    val times = timeSomes.map(_.get)
    assertEmpty(supposedlyEmptys.filter(_.isDefined).toSet)
    Participant(
      id = num.toInt,
      firstName = firstName,
      lastName = lastName,
      nickOption = Some(nick).filter(_ != ""),
      gender = genderString match {
        case "m" => Gender.Male
        case "ž" => Gender.Female
      },
      age = if(formatVersion.ageType == AgeType.Category) ageString else "",
      birthYear = if(formatVersion.ageType == AgeType.BirthYear) Some(ageString.toInt) else None,
      last3 = last3,
      partTimes = times
    )
  }
}
