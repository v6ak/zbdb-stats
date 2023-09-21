package com.example

import com.example.moment.Moment

import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native trait MomentSingleton extends js.Any {
  //def moment(): Moment = js.native
  def utc(): Moment = js.native
  def utc(x: Number): Moment = js.native
  //def utc(xNumber[]): Moment = js.native
  def utc(time: String): Moment = js.native
  /*def moment.utc(String, String): Moment = js.native
  def moment.utc(String, String[]): Moment = js.native
  def moment.utc(String, String, String): Moment = js.native
  def moment.utc(Moment): Moment = js.native
  def moment.utc(Date): Moment = js.native*/
  def tz(time: String, tz: String): Moment = js.native
}

@js.native
@JSGlobalScope
object MomentJsGlobal extends js.Any {
  def moment(moment: Moment): Moment = js.native
  def moment(dateString: String): Moment = js.native
  def moment(dateString: String, format: String): Moment = js.native
  def moment(dateString: String, format: String, locale: String): Moment = js.native
  def moment(dateString: String, format: String, strict: Boolean): Moment = js.native
  def moment(dateString: String, format: String, locale: String, strict: Boolean): Moment = js.native

  def moment: MomentSingleton = js.native

}

package object moment {
  @inline def moment(moment: Moment): Moment = MomentJsGlobal.moment(moment: Moment)
  @inline def moment(dateString: String): Moment = MomentJsGlobal.moment(dateString: String)
  @inline def moment(dateString: String, format: String): Moment = MomentJsGlobal.moment(dateString: String, format: String)
  @inline def moment(dateString: String, format: String, locale: String): Moment = MomentJsGlobal.moment(dateString: String, format: String, locale: String)
  @inline def moment(dateString: String, format: String, strict: Boolean): Moment = MomentJsGlobal.moment(dateString: String, format: String, strict: Boolean)
  @inline def moment(dateString: String, format: String, locale: String, strict: Boolean): Moment = MomentJsGlobal.moment(dateString: String, format: String, locale: String, strict: Boolean)
  @inline def moment: MomentSingleton = MomentJsGlobal.moment
}

package moment{



import scala.scalajs.js.Date

@js.native
trait Moment extends js.Any {
  def add(time: Int, units: String): Moment = js.native
  //def plus(time: Int, units: String): Moment = js.native


  def isValid(): Boolean = js.native

    def format(): String = js.native

    def hours(): Int = js.native
    def hours(v: Int): Moment = js.native
    def minutes(): Int = js.native
    def minutes(v: Int): Moment = js.native
    def seconds(): Int = js.native
    def minus(other: Moment): Int = js.native
    @JSOperator def -(other: Moment): Int = js.native
    def plus(other: Int): Moment = js.native
    @JSOperator def +(other: Int): Moment = js.native

    def date(): Int = js.native
    def date(v: Int): Moment = js.native

    def toDate(): Date = js.native

    def isBefore(other: Moment): Boolean = js.native
    def isBefore(other: Moment, precision: String): Boolean = js.native
    def isAfter(other: Moment): Boolean = js.native
    def isAfter(other: Moment, precision: String): Boolean = js.native

    def isSame(other: Moment): Boolean = js.native
    //def clone(): Moment = js.native

    //def milliseconds(): Int = js.native
    def unix(): Int = js.native

  }

}

object Moment {
  implicit val MomentOrdering: Ordering[Moment] = Ordering.by(_.unix())
}

class RichMoment(val moment: Moment) extends AnyVal{

  def >=(other: Moment):Boolean = moment.isSame(other) || (moment isAfter other)
  def <=(other: Moment):Boolean = moment.isSame(other) || (moment isBefore other)
  def >(other: Moment): Boolean = moment isAfter other
  def <(other: Moment): Boolean = moment isBefore other

  def hoursAndMinutes = f"${moment.hours()}%d:${moment.minutes()}%02d"

}

object RichMoment{
  implicit def toRichMoment(moment: Moment): RichMoment = new RichMoment(moment)
}