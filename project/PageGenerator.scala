import spray.json.{CompactPrinter, JsArray, JsObject, JsString, JsonPrinter}

import scala.xml.Text

object PageGenerator{

  sealed abstract class DataSource{
    def csvAjaxUrl: String
    def csvDownloadUrl: String
    def originalLink: String
    def prefetchAjax: Boolean
    def plots: Seq[(String, String)]
  }
  trait CorsAnywhereDataSource extends DataSource {
    override def csvAjaxUrl: String = s"https://cors-anywhere.herokuapp.com/$csvDownloadUrl"
    override def prefetchAjax: Boolean = false
  }
  final case class GoogleSpreadsheetDataSource(key: String, gid: Int = 0, plotIds: Seq[(String, Int)]) extends DataSource with CorsAnywhereDataSource{
    private def pubhtml(sheetId: Int): String = s"https://docs.google.com/spreadsheets/d/$key/pubhtml?gid=$sheetId"
    override def csvDownloadUrl: String = s"https://docs.google.com/spreadsheets/d/$key/pub?gid=$gid&single=true&output=csv"
    override def originalLink: String = pubhtml(gid)
    override def plots: Seq[(String, String)] = plotIds.map{case (name, sheetId) => (name, pubhtml(sheetId))}
  }
  final case class NewGoogleSpreadsheetDataSource(key: String/*, plotIds: Seq[(String, Int)]*/) extends DataSource with CorsAnywhereDataSource{
    private def url(format: String): String = s"https://docs.google.com/spreadsheets/d/e/$key/pub?output=$format"
    override def csvDownloadUrl: String = url("csv")
    override def originalLink: String = url("html")
    override def plots: Seq[(String, String)] = Seq() //not supported yet; plotIds.map{case (name, sheetId) => (name, pubhtml(sheetId))}
  }
  final case class FileDataSource(file: String, originalLink: String) extends DataSource{
    override def csvAjaxUrl: String = file
    override def csvDownloadUrl: String = file
    override def prefetchAjax: Boolean = true
    override def plots: Seq[(String, String)] = Seq()
  }

  case class Year(year: Int, formatVersion: Int, dataSource: DataSource, startTime: String, endTime: String, additionalAlternativeLinks: Seq[(String, String)] = Seq(), maxHoursDelta: Int = 6){
    def alternativeLinks = Seq("Tabulka Google" -> dataSource.originalLink, "CSV" -> dataSource.csvDownloadUrl) ++ additionalAlternativeLinks
  }

  val LegacyYears = Seq(
    2011 -> "http://zbdb.skaut1stredisko.cz/2011/vysledky-16-rocniku-pochodu-z-brna-do-brna/",
    2012 -> "http://zbdb.skaut1stredisko.cz/2012/vysledky-17-rocniku-pochodu-z-brna-do-brna/",
    2013 -> "http://zbdb.skaut1stredisko.cz/2013/vysledky-18-rocniku/",
    2014 -> "http://zbdb.skaut1stredisko.cz/2014/vysledky-19-rocniku/",
    2015 -> "http://zbdb.skaut1stredisko.cz/2015/vysledky-pochodu-20-rocniku/"
  )

  val Years = Seq(
    Year(
      year = 2015, formatVersion = 2015,
      startTime = "2015-09-18 17:30", endTime = "2015-09-19 19:00",
      dataSource = FileDataSource("zbdb-2015-simplified.csv", "https://docs.google.com/spreadsheets/d/1sCOP6tEAQmjkdRhfrgOd0RLXAlgZ_0NiXAU1prcXqP0/pubhtml?gid=1935861499&single=true")
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
    )
  )

  val YearLinks = LegacyYears ++ Years.map(y => y.year -> s"../../${y.year}/statistiky/")

  def allYearsJsonString = CompactPrinter.apply(JsObject(YearLinks.map{case (y, link) => s"$y"->JsString(link)}.toMap))

  def forYear(year: Year, publicDirName: String) = {
    val title = s"Výsledky Z Brna do Brna ${year.year}"
    val plots = CompactPrinter.apply(JsArray(year.dataSource.plots.map{case (x, y) => JsArray(JsString(x), JsString(y))}: _*))
    "<!DOCTYPE html>"+
    <html>
      <head>
        <meta charset="utf-8" />
        <link rel="stylesheet" type="text/css" href={s"../../$publicDirName/main.min.css"} />
        <script type="text/javascript" src={s"../../$publicDirName/main.min.js"}></script>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        {if(year.dataSource.prefetchAjax) <link rel="prefetch" href={year.dataSource.csvAjaxUrl} /> else ""}
        <meta http-equiv="X-UA-Compatible" content="IE=10; IE=9; IE=8; IE=7; IE=EDGE" />
        <title>{title}</title>
      </head>
      <body
        data-plots={plots}
        data-file={year.dataSource.csvAjaxUrl}
        data-start-time={year.startTime}
        data-end-time={year.endTime}
        data-timezone="Europe/Prague"
        data-max-hour-delta={year.maxHoursDelta.toString}
        data-format-version={year.formatVersion.toString}
        data-year={year.year.toString}
      >
        <div class="container">
          <h1>{title}</h1>
          <p class="hidden-print">Alternativní podoby: {year.alternativeLinks.flatMap{case(name, link) =>
            Seq(Text(", "), <a href={link}>{name}</a>)
          }.tail}</p>
        </div>
        <div id="content"><div id="loading-indicator">
          <div class="progress progress-striped active">
            <div class="progress-bar" role="progressbar" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100" style="width: 100%">Načítám výsledky…</div>
          </div>
        </div></div>
        <div class="container hidden-print">
          <h2>Když něco nefunguje…</h2>
          <p>Mělo by to fungovat v moderních prohlížečích. Pokud bude nějaký problém, <a href="https://contact.v6ak.com/">napiš mi</a> a uveď použitý webový prohlížeč. Nepodporuju ale:</p>
          <ul>
            <li>Archaické verze prohlížečů</li>
            <li>Malý displej – není úplně jednoduché to udělat na malém displeji dobře použitelné</li>
            <li>Prohlížeč Opera Mini – dost specifický prohlížeč vhodný spíše na jednoduché stránky</li>
            <li>Obskurní prohlížeče jako Lynx apod. :)</li>
          </ul>
          <p>Zdrojové kódy jsou <a href="https://github.com/v6ak/zbdb-stats">na GitHubu</a>.</p>
        </div>
      </body>
    </html>
  }
}
