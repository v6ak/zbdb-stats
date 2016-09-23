package com.v6ak.zbdb

object Assertions{

  def assertEmpty(set: Set[_]) = if(set.nonEmpty){
    sys.error("Following set is not empty: "+set)
  }

  def assertEquals[T](a: T, b:T) = if(a != b){
    sys.error("Following values are not equal: "+a+", "+b)
  }

}
