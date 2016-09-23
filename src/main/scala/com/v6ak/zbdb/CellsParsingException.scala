package com.v6ak.zbdb

case class CellsParsingException(data: Seq[String], e: Throwable) extends RuntimeException(s"Error when parsing $data", e)