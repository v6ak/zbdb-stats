package com.v6ak.scalajs.tables

import org.scalajs.dom.html.TableRow

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

final class TableRenderer[T](headRows: Int = 1, tableModifiers: Seq[Modifier] = Seq(), trWrapper: (TypedTag[TableRow], T) => TypedTag[TableRow] = {(a: TypedTag[TableRow], _: T) => a})(columns: Seq[Column[T]]){

  columns foreach { c =>
    if(c.rowCount > headRows){  // TODO: consider !=; however, I am not sure about multiple-row cells
      sys.error(s"bad rowCount ${c.rowCount} for $c")
    }
  }

  def renderTableHead = thead(0 until headRows map { headRowIndex =>
    tr(columns.map{col =>
      col.renderHeader(headRowIndex).map{ tableHeadColumn =>
        tableHeadColumn.create(headRows)
      }
    })
  })

  def renderTableBody(data: Seq[T]) = tbody(data.map(row => trWrapper(tr(columns.map(col => col.createContentCell(row))), row)))

  def renderTable(data: Seq[T]) = table(
    renderTableHead,
    renderTableBody(data)
  )(
    tableModifiers : _*
  ).render

}
