package com.v6ak.scalajs.tables

import org.scalajs.dom.html.TableRow
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.*

final class TableRenderer[T](
  headRows: Int = 1,
  tableModifiers: Seq[Modifier] = Seq(),
  trWrapper: (TypedTag[TableRow], T) => TypedTag[TableRow] = {(a: TypedTag[TableRow], _: T) => a},
)(columns: Seq[Column[T]]):

  columns foreach: c =>
    if(c.rowCount > headRows)  // TODO: consider !=; however, I am not sure about multiple-row cells
      sys.error(s"bad rowCount ${c.rowCount} for $c")

  private def colMarkers(pos: Int, count: Int) =
    val range = pos until (pos + count)
    range.map{ i => `class` := s"col-$i" } :+ (data.cols := range.mkString(" "))

  def renderTableHead = thead(0 until headRows map { headRowIndex =>
    tr(columns.zipWithIndex.map{ (col, colPos) =>
      col.renderHeader(headRowIndex).map{ tableHeadColumn =>
        tableHeadColumn.create(headRows)(colMarkers(colPos, tableHeadColumn.colCount))
      }
    })
  })

  def renderTableBody(data: Seq[T]) = tbody(
    data.zipWithIndex.map( (row, rowPos) =>
      trWrapper(
        tr(
          columns.zipWithIndex.map( (col, colPos) =>
            col.createContentCell(row, rowPos, data.size) match
              case tag: TypedTag[_] => tag(colMarkers(colPos, 1))
              case other => other
          )
        ),
        row
      )
    )
  )

  def renderTable(data: Seq[T]) = table(
    renderTableHead,
    renderTableBody(data),
    tableModifiers,
  ).render
