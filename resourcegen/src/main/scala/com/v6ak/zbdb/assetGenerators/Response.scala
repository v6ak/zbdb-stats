package com.v6ak.zbdb.assetGenerators

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal

sealed abstract class Response:
  def respond(response: js.Dynamic, next: js.Function0[js.Any]): Unit


object Response:
  case class Real(contentType: String, content: String) extends Response:
    override def respond(response: js.Dynamic, next: js.Function0[js.Any]): Unit =
      response.writeHead(200, literal(`content-type` = contentType))
      response.end(content)

  case object Next extends Response:
    override def respond(response: js.Dynamic, next: js.Function0[js.Any]): Unit = next()

  def html(s: String) = Real("text/html; encoding=utf-8", s)
  def json(s: String) = Real("application/json; encoding=utf-8", s)
  def csv(s: String) = Real("text/csv; encoding=utf-8", s)

