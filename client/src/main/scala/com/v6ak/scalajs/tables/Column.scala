package com.v6ak.scalajs.tables

import org.scalajs.dom.Node

import scalatags.JsDom.all
import scalatags.JsDom.all._


final case class TableHeadColumn(content: Node, colCount: Int = 1, rowCountOption: Option[Int] = None, className: String) {
  def create(headRows: Int) = th(
    `class` := className,
    colspan := colCount,
    rowspan := rowCountOption.getOrElse(headRows),
    content
  )

}

abstract sealed class TableHeadCell{
  def rowCount: Int
  def build(className: String): Option[TableHeadColumn]
}

object TableHeadCell{
  //def apply(content: Node, colCount: Int = 1, rowCount: Int = 1) = Full(content = content, colCount = colCount, rowCount = rowCount)
  def apply(frag: Frag, colCount: Int = 1, rowCount: Int = 1) = Full(content = frag.render, colCount = colCount, rowCount = rowCount)
  final case class Full(content: Node, colCount: Int, rowCount: Int) extends TableHeadCell{
    def build(className: String) = Some(TableHeadColumn(content = content, colCount = colCount, rowCountOption = Some(rowCount), className = className))
  }
  case object Empty extends TableHeadCell{
    override def rowCount: Int = 0
    override def build(className: String): Option[TableHeadColumn] = None
  }
}

object Column{

  def apply[T](header: Frag)(cellRenderer: T => Frag) = new Column[T] {
    override def rowCount: Int = 1
    override def renderHeader(i: Int): Option[TableHeadColumn] = i match {
      case 0 => Some(TableHeadColumn(content = header.render, colCount = 1, rowCountOption = None, className = ""))
      case _ => None
    }
    private def renderContent(data: T): Node = cellRenderer(data).render
    override def createContentCell(row: T): Frag = td(renderContent(row))
  }

  def apply[T](headers: TableHeadCell*)(cellRenderer: T => Frag)(className: String = "") = new Column[T] {
    override def rowCount: Int = headers.map(_.rowCount).sum
    override def renderHeader(i: Int): Option[TableHeadColumn] = headers.lift(i).flatMap(_.build(className = className))
    private def renderContent(data: T): Node = cellRenderer(data).render
    override def createContentCell(row: T): Frag = td(`class` := className, renderContent(row))
  }

  def rich[T](headers: TableHeadCell*)(cellRenderer: T => Seq[Modifier])(className: String = "") = new Column[T] {
    override def rowCount: Int = headers.map(_.rowCount).sum
    override def renderHeader(i: Int): Option[TableHeadColumn] = headers.lift(i).flatMap(_.build(className = className))
    private def renderContent(data: T): Modifier = cellRenderer(data)
    override def createContentCell(row: T): Frag = td(`class` := className, renderContent(row))
  }

}

abstract class Column[T]{
  def rowCount: Int
  def renderHeader(i: Int): Option[TableHeadColumn]
  def createContentCell(row: T): Frag
}
