package com.example

import com.example.moment.Moment

import scalajs.js

@js.native class MomentSingleton extends js.Any{
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

package object moment extends scalajs.js.GlobalScope {


  def moment(moment: Moment): Moment = js.native
  def moment(dateString: String): Moment = js.native
  def moment(dateString: String, format: String): Moment = js.native
  def moment(dateString: String, format: String, locale: String): Moment = js.native
  def moment(dateString: String, format: String, strict: Boolean): Moment = js.native
  def moment(dateString: String, format: String, locale: String, strict: Boolean): Moment = js.native

  def moment: MomentSingleton = js.native

}

package moment{

import scala.scalajs.js.Date

@js.native
class Moment extends js.Any {
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
    def -(other: Moment): Int = js.native
    def plus(other: Int): Moment = js.native
    def +(other: Int): Moment = js.native

    def date(): Int = js.native
    def date(v: Int): Moment = js.native

    def toDate(): Date = js.native

    def isBefore(other: Moment): Boolean = js.native
    def isBefore(other: Moment, precision: String): Boolean = js.native
    def isAfter(other: Moment): Boolean = js.native
    def isAfter(other: Moment, precision: String): Boolean = js.native

    def >=(other: Moment) = isSame(other) || (this isAfter other)
    def <=(other: Moment) = isSame(other) || (this isBefore other)

    def isSame(other: Moment): Boolean = js.native
    //def clone(): Moment = js.native


  }

}


