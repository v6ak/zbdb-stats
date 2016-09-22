package com.v6ak.zbdb

abstract sealed class Gender()

object Gender{
  case object Male extends Gender
  case object Female extends Gender
}