import spray.json.{CompactPrinter, JsArray, JsObject, JsString, JsonPrinter}

import scalatags.Text.all.{ html => htmlTag, _ }
import scalatags.Text.tags2.{ title => titleTag }


object PageGenerator{

  sealed abstract class DataSource{
    def csvDownloadUrl: String
    def originalLink: String
    def plots: Seq[(String, String)]
  }
  final case class GoogleSpreadsheetDataSource(key: String, gid: Int = 0, plotIds: Seq[(String, Int)]) extends DataSource {
    private def pubhtml(sheetId: Int): String = s"https://docs.google.com/spreadsheets/d/$key/pubhtml?gid=$sheetId"
    override def csvDownloadUrl: String = s"https://docs.google.com/spreadsheets/d/$key/pub?gid=$gid&single=true&output=csv"
    override def originalLink: String = pubhtml(gid)
    override def plots: Seq[(String, String)] = plotIds.map{case (name, sheetId) => (name, pubhtml(sheetId))}
  }
  final case class NewGoogleSpreadsheetDataSource(key: String/*, plotIds: Seq[(String, Int)]*/) extends DataSource {
    private def url(format: String): String = s"https://docs.google.com/spreadsheets/d/e/$key/pub?output=$format"
    override def csvDownloadUrl: String = url("csv")
    override def originalLink: String = url("html")
    override def plots: Seq[(String, String)] = Seq() //not supported yet; plotIds.map{case (name, sheetId) => (name, pubhtml(sheetId))}
  }
  final case class FileDataSource(file: String, originalLink: String) extends DataSource{
    // Works well only if file is <year>.csv. I could fix it, but it is legacy DataSource and it fits the needs well.
    override def csvDownloadUrl: String = s"$file"
    override def plots: Seq[(String, String)] = Seq()
  }

  case class Year(year: Int, formatVersion: Int, dataSource: DataSource, startTime: String, endTime: String, additionalAlternativeLinks: Seq[(String, String)] = Seq(), maxHoursDelta: Int = 6){
    def alternativeLinks = Seq("Tabulka Google" -> dataSource.originalLink, "CSV" -> dataSource.csvDownloadUrl) ++ additionalAlternativeLinks
  }

  val LegacyYears = Seq(
    2011 -> "https://zbdb.skauting.cz/2011/vysledky-16-rocniku-pochodu-z-brna-do-brna/",
    2012 -> "https://zbdb.skauting.cz/2012/vysledky-17-rocniku-pochodu-z-brna-do-brna/",
    2013 -> "https://zbdb.skauting.cz/2013/vysledky-18-rocniku/",
    2014 -> "https://zbdb.skauting.cz/2014/vysledky-19-rocniku/",
    2015 -> "https://zbdb.skauting.cz/2015/vysledky-pochodu-20-rocniku/"
  )

  val Years = Seq(
    Year(
      year = 2015, formatVersion = 2015,
      startTime = "2015-09-18 17:30", endTime = "2015-09-19 19:00",
      dataSource = FileDataSource("2015.csv", "https://docs.google.com/spreadsheets/d/1sCOP6tEAQmjkdRhfrgOd0RLXAlgZ_0NiXAU1prcXqP0/pubhtml?gid=1935861499&single=true")
    ),
    Year(
      year = 2016, formatVersion = 2016,
      startTime = "2016-09-16 17:30", endTime="2016-09-17 20:00",
      dataSource = GoogleSpreadsheetDataSource(
        key = "1Ijx8bsvSkCh27rnD7sAxfN5TLDDjSX5rMvp63MnnKFs",
        gid = 1919778948,
        plotIds = Seq(
          "Věková struktura" -> 664145624,
          "Časy stanovišť" -> 1256165661
        )
      ),
      additionalAlternativeLinks = Seq(
        "PDF" -> "https://docs.google.com/spreadsheets/d/1Ijx8bsvSkCh27rnD7sAxfN5TLDDjSX5rMvp63MnnKFs/pub?output=pdf"
      )
    ),
    Year(
      year = 2017, formatVersion = 2017,
      startTime = "2017-09-15 17:30", endTime = "2017-09-16 20:00",
      dataSource = NewGoogleSpreadsheetDataSource("2PACX-1vRb0q-j8kDjMk3ptlqdwjNTXDhpMAjc-fPc7JGoYdnpxrQ30lXAzhLgcXpvMHR_XgwxKbTr-NMZv0UE")
    ),
    Year(
      year = 2018, formatVersion = 2017,
      startTime = "2018-09-21 17:30", endTime = "2018-09-22 21:00",
      dataSource = NewGoogleSpreadsheetDataSource("2PACX-1vQyyPGYg-MeFN9nRpHCkog0GKd2h3sSV1lI-EYwMZckmf1w2IV3FSyObWC4GPDKAY6Tu3s46ILm3gN4")
    ),
    Year(
      year = 2019, formatVersion = 2017,
      startTime = "2019-09-20 17:30", endTime = "2019-09-21 21:00",
      dataSource = NewGoogleSpreadsheetDataSource("2PACX-1vTZSX8UVnXyoF8PeiQH-L1RvoBogXw8WlXn7oCkogWG2QPP2ZDc4PVNy3Lz6FDyzXqL_Ep78cZgSiCQ")
    ),
    Year(
      year = 2021, formatVersion = 2021,
      startTime = "2021-09-17 17:05", endTime = "2021-09-18 20:05",
      dataSource= NewGoogleSpreadsheetDataSource("2PACX-1vSo5X1l36As8yB9-XRquV9UGIAcptWSzm7P7bEIoj93WVcEhwYumOKOG6h3O147IASNhAJrVwd-CKDq")
    ),
    Year(
      year = 2022, formatVersion = 2021,
      startTime = "2022-09-16 17:30", endTime = "2022-09-17 20:00",
      dataSource= NewGoogleSpreadsheetDataSource("2PACX-1vQEmRVRc1DBm9PZoRU-4oKu0p6gTWqv6lYbvvrDwGT-umiXtB4Xy13NEcFeanZ37PTw2UrN8TYaHK15")
    ),
    Year(
      year = 2023, formatVersion = 2021,
      startTime = "2023-09-15 17:30", endTime = "2023-09-16 18:10",
      dataSource= NewGoogleSpreadsheetDataSource("2PACX-1vTzrUrHEarwmtqap2WZQRMJvO7UVy6rGln2xuZv5kWa_slIM6c_-p7BasSUkipAJs86iIOwWDtJlrlb")
    ),
    Year(
      year = 2024, formatVersion = 2021,
      startTime = "2024-10-11 17:30", endTime = "2024-10-12 20:00",
      dataSource= NewGoogleSpreadsheetDataSource("2PACX-1vRwN8Cyp9ONHwqFsRkZOHnoyCCFUq_wy75upVVugJ9DgoyGjjO3zhOjvUfDDWhy7O5ETDH9GJIPLvwO")
    ),
    Year(
      year = 2025, formatVersion = 2021,
      startTime = "2025-09-12 17:30", endTime = "2025-09-13 20:00",
      dataSource= NewGoogleSpreadsheetDataSource("2PACX-1vQCGArcec5pYm6Pt3XIesXrr0blMu8I-UcDA82pm8CdzUpmfL_ProD2x7Qou2L-Igy79s5zfVNXnlDp")
    ),
  )

  val YearLinks = LegacyYears ++ Years.map(y => y.year -> s"../../${y.year}/statistiky/")

  def allYearsJsonString = CompactPrinter.apply(JsObject(YearLinks.map{case (y, link) => s"$y"->JsString(link)}.toMap))

  def forYear(year: Year, publicDirName: String): String = {
    val pageTitle = s"Výsledky Z Brna do Brna ${year.year}"
    val plots = CompactPrinter.apply(JsArray(year.dataSource.plots.map{case (x, y) => JsArray(JsString(x), JsString(y))}: _*))
    val csvFile = s"${year.year}.csv"
    "<!DOCTYPE html>"+
    htmlTag(
      head(
        meta(charset := "utf-8"),
        link(rel := "stylesheet", `type`:="text/css", href:=s"../../$publicDirName/main.css"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1.0, minimum-scale=1.0"),
        link(rel := "prefetch", href := csvFile),
        meta(attr("http-equiv") := "X-UA-Compatible", content := "IE=10; IE=9; IE=8; IE=7; IE=EDGE"),
        titleTag(pageTitle),
      ),
      body(
        data.plots := plots,
        data.file := csvFile,
        data.`start-time` := year.startTime,
        data.`end-time` := year.endTime,
        data.timezone := "Europe/Prague",
        data.`max-hour-delta` := year.maxHoursDelta.toString,
        data.`format-version` := year.formatVersion.toString,
        data.year := year.year.toString,
      )(
        div(cls := "container",
          h1(pageTitle),
          p(cls := "d-print-none")(
            "Alternativní podoby: ",
            year.alternativeLinks.flatMap{case(name, link) =>
              Seq(
                ", ": Frag,
                a(href := link, name),
              )
            }.tail,
          ),
        ),
        div(id := "content")(
          div(id := "loading-indicator")(
            div(cls := "progress progress-striped active")(
              div(
                cls := "progress-bar progress-bar-striped progress-bar-animated",
                role := "progressbar",
                aria.valuenow := "100",
                aria.valuemin := "0",
                aria.valuemax := "100",
                style := "width: 100%",
              )(
                "Načítám výsledky…",
              ),
            ),
          ),
        ),
        div(cls := "container d-print-none")(
          h2("Když něco nefunguje…"),
          p(
            "Mělo by to fungovat v moderních prohlížečích. Pokud bude nějaký problém, ",
            a(href := "https://contact.v6ak.com/", "napiš mi"),
            " a uveď použitý webový prohlížeč.",
          ),
          p(
            "Zdrojové kódy jsou ",
            a(href := "https://github.com/v6ak/zbdb-stats", "na GitHubu"),
            ".",
          ),
        ),
        script(`type` := "text/javascript", src := s"../../$publicDirName/main.min.js"),
      ),
    )
  }
}
