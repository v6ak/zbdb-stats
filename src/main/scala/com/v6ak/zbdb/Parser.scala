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

  private def parseTime(s: String, prevTime: Moment, maxHourDelta: Int) = s match {
    case TimeRegexp(hs, ms) =>
      if(!prevTime.isValid()){
        sys.error("invalid prevTime: "+prevTime)
      }
      val result = moment(prevTime)
      result.hours(hs.toInt)
      result.minutes(ms.toInt)
      if(result isBefore prevTime){
        result.date(result.date + 1)
      }
      if(!result.isValid()){
        sys.error("invalid date: "+s)
      }
      strictCheck{
        if((result.toDate().getTime() - prevTime.toDate().getTime()) > (maxHourDelta*3600*1000)){
          sys.error(s"following time specification overreaches time delta $maxHourDelta hours ($prevTime -> $result): $s")
        }
      }
      result
  }

  private def guard[T](data: Any)(f: => T) = try{
    f
  }catch{
    case e: Throwable => throw new RuntimeException(s"Bad format near $data", e)
  }

  private def parseTimeInfo(data: Seq[String], prevTimeOption: Option[Moment], maxHourDelta: Int): Option[PartTimeInfo] = guard(data){
    data match {
      case Seq("X"|"x"|"", "X"|"x"|"", "X"|"x"|"") => None
      case Seq(startTimeString, "X"|"x"|"", "X"|"x"|"") =>
        val prevTime = prevTimeOption.get
        val startTime = parseTime(startTimeString, prevTime, maxHourDelta)
        Some(PartTimeInfo.Unfinished(startTime))
      case Seq(startTimeString, intervalTimeString, endTimeString) =>
        val prevTime = prevTimeOption.get
        val startTime = parseTime(startTimeString, prevTime, maxHourDelta)
        val endTime = parseTime(endTimeString, startTime, maxHourDelta)
        val intervalTime = TimeInterval.parse(intervalTimeString)
        assert( (endTime.toDate.getTime - startTime.toDate.getTime) == (intervalTime.totalMinutes*60*1000) )
        Some(PartTimeInfo.Finished(
          startTime = startTime,
          endTime = endTime,
          intervalTime = intervalTime
        ))
    }
  }

  def parse(csvData: String, startTime: Moment, totalEndTime: Moment, maxHourDelta: Int, formatVersion: Int) = {
    val fullDataTable = new CSVReader(new StringReader(csvData.trim)).toIndexedSeq.map(_.toIndexedSeq)
    val Seq(title, header1, header2, header3, dataWithTail @ _*) = fullDataTable
    val (headLength, tailLength) = formatVersion match {
      case 2015 => (0, 2)
      case 2016 => (1, 0)
    }
    val dataTable = dataWithTail.drop(headLength).dropRight(tailLength)
    dom.console.log("dataTable", dataTable.toString())
    if(tailLength == 2){
      val Seq(footer1, footer2) = dataWithTail.takeRight(2)
      dom.console.log("footer1", footer1.toString())
      dom.console.log("footer2", footer2.toString())
      assertEmpty(footer1.take(5).toSet -- Set(""))
      assertEmpty(footer2.toSet.filterNot(_.forall(_.isDigit)) -- Set("", "nejdříve na stanovišti", "nejrychleji projitý úsek", "Na trati"))
    }
    val parts = (header1, header2, header3).zipped.toIndexedSeq.drop(6).dropRight(4).grouped(3).map{ case Seq((""|"čas startu", "", "odch"), (" =>"|"=>", trackLengthString, ""), (place, cumulativeTrackLengthString, "přích")) =>
      Part(
        place = place,
        trackLength = parseTrackLength(trackLengthString),
        cumulativeTrackLength = parseTrackLength(cumulativeTrackLengthString)
      )
    }.toIndexedSeq
    val parsedDataTries = dataTable.map{participantData =>
      Try(
        Left(parseParticipant(participantData, parts, startTime, maxHourDelta, totalEndTime = totalEndTime, formatVersion = formatVersion))
      ).recover{
        case e: Throwable => Right(participantData, e)
      }.get
    }
    val (parsedDataSuccessfulTries, parsedDataFailedTries) = parsedDataTries.partition(_.isLeft)
    /*if(parsedDataFailedTries.nonEmpty){
      dom.console.error(s"parsing some data failed (${parsedDataFailedTries.size}):")
      parsedDataFailedTries foreach { case Right((data, e)) =>
        dom.console.error("following row is not successfuly parsed:", js.Array(data : _*))
        e.printStackTrace()
      }
      sys.error("some data failed to parse")
    }*/
    (parts, parsedDataSuccessfulTries.map{case Left(p) => p}, parsedDataFailedTries.map{case Right((row, e)) => (row, e)})
  }

  private def parseParticipant(participantData: immutable.IndexedSeq[String], parts: immutable.IndexedSeq[Part], startTime: Moment, maxHourDelta: Int, totalEndTime: Moment, formatVersion: Int): Participant = {
    val Seq(num, lastName, firstName, nick, genderString, ageString, other@_*) = participantData
    if ((formatVersion <= 2015) && (ageString != "")) {
      sys.error("You seem to have left some data in age column. Clean it first, please.")
    }
    val timesUnparsed = other.dropRight(4).grouped(3).toIndexedSeq
    val last3 = other.takeRight(4)
    //dom.console.log("unparsedTimes", timesUnparsed.toString, timesUnparsed.size)
    val timeOptions = timesUnparsed.tail.scanLeft(parseTimeInfo(timesUnparsed.head, Some(startTime), maxHourDelta)) { (previous, data) =>
      parseTimeInfo(data, previous.collect { case i: Finished => i.endTime }, maxHourDelta)
    }
    strictCheck{
      assertEquals(timeOptions.filter(_.exists{ti => (ti.startTime isAfter totalEndTime) || ti.endTimeOption.exists(endTime => endTime isAfter totalEndTime)}), Seq())
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
      //age = ageString,
      last3 = last3,
      partTimes = times
    )
  }
}
