package com.v6ak.zbdb.assetGenerators

import org.scalajs.dom.{console, fetch}
import scalatags.Text.all.{html as htmlTag, *}
import scalatags.Text.tags2.title as titleTag

import java.io.IOException
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.*
import scala.scalajs.js.Thenable.Implicits.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.JSImport.Namespace

@JSExportTopLevel("Server")
object Server:

  @js.native
  @JSImport("fs", Namespace)
  val fs: js.Dynamic = js.native

  @js.native
  @JSImport("path", Namespace)
  val path: js.Dynamic = js.native

  private val CsvFile = """^/([0-9]+)/statistiky/\1.csv$""".r
  private val HtmlFile = """^/([0-9]+)/statistiky/(?:index\.html|)$""".r //
  private val YearsJson = """^/statistiky/years\.json$""".r

  @JSExport
  def handle(req: js.Dynamic, res: js.Dynamic, next: js.Function0[js.Any]): Promise[Unit] =
    resultFor.lift(req.url.asInstanceOf[String])
      .getOrElse(Promise.resolve(Response.Next))
      .`then`(_.respond(res, next))

  def download(out: String, source: String): Promise[String] = {
    if (fs.existsSync(out).asInstanceOf[Boolean]) {
      println(s"Source $source is already downloaded at $out.")
      Promise.resolve(out)
    } else {
      val tmpFile = out + ".tmp"
      println(s"Downloading $source…")
      val parentDir = path.dirname(out)
      console.log("parentDir", parentDir)
      fs.promises.mkdir(parentDir, literal(recursive=true)).asInstanceOf[Promise[Unit]].`then`(_ =>
        console.log("mkdir done")
        fetch(source).`then`(_.text()).`then`(res =>
          console.log("fetch res", res)
          fs.promises.writeFile(tmpFile, res).asInstanceOf[Promise[Unit]].`then`( _ =>
            fs.promises.rename(tmpFile, out).asInstanceOf[Promise[Unit]].`then`( _ =>
              println(s"Downloaded $source as $out")
              out
            )
          )
        )
      )
    }
  }

  def resultFor: PartialFunction[String, Promise[Response]] =
    case HtmlFile(IntVal(year)) =>
      val html = PageGenerator.forYear(year, "statistiky")
      Promise.resolve(Response.html(html))
    case CsvFile(IntVal(year)) =>
      val dataSource = PageGenerator.YearsByNumber(year).dataSource
      console.log("csv", year)
      if (dataSource.csvNeedsDownload)
        val cacheFile = s"tmp/$year.csv"
        download(cacheFile, dataSource.csvDownloadUrl).`then`( file =>
          fs.promises.readFile(file, "utf-8").asInstanceOf[Promise[String]].`then`( (data: String) =>
            Response.csv(data)
          )
        )
      else
        Promise.resolve(Response.Next)
    case YearsJson() =>
      Promise.resolve(Response.json(PageGenerator.allYearsJsonString))

  def allUrls: Seq[String] =
    val perYear = for (
      year <- PageGenerator.Years
    ) yield Seq(
      Some(s"/${year.year}/statistiky/index.html"),
      if year.dataSource.csvNeedsDownload then Some(s"/${year.year}/statistiky/${year.year}.csv") else None,
    ).flatten
    val global = Seq("/statistiky/years.json")
    global ++ perYear.flatten
