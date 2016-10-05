package com.v6ak.zbdb

object IdGenerator{

  private var i = 0

  def newId() = {
    i += 1
    "generated-id-"+i
  }

}