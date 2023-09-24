package com.v6ak.zbdb

abstract sealed class Gender(){
  def inflect[T](feminine:T, masculine: T): T
}

object Gender{
  case object Male extends Gender {
    override def inflect[T](feminine: T, masculine: T): T = masculine
  }
  case object Female extends Gender {
    override def inflect[T](feminine: T, masculine: T): T = feminine
  }
}