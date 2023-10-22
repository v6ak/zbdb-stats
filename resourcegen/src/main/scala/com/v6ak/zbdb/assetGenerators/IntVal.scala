package com.v6ak.zbdb.assetGenerators

object IntVal:
  def unapply(s: String): Option[Int] = s.toIntOption
