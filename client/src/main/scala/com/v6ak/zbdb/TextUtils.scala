package com.v6ak.zbdb

object TextUtils {

  val Genders = Map[Gender, String](Gender.Male -> "♂", Gender.Female -> "♀")

  def formatLength(length: BigDecimal, space: String = " ") = length.toString().replace('.', ',') + space + "km"

  def formatSpeed(speedInKmPerH: BigDecimal) = f"$speedInKmPerH%1.2f km/h".replace('.', ',')

}
