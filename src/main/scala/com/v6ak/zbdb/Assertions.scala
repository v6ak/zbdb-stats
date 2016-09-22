package com.v6ak.zbdb

object Assertions{

  def assertEmpty(set: Set[Any]) = if(!set.isEmpty){
    sys.error("Following set is not empty: "+set)
  }

  def assertEquals[T](a: T, b:T) = if(a != b){
    sys.error("Following values are not equal: "+a+", "+b)
  }

}
