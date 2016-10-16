import spray.json.{CompactPrinter, JsArray, JsString, JsonPrinter}

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
    //override def csvAjaxUrl: String = s"https://zbdb.v6ak.com/spreadsheets/$key"
    override def plots: Seq[(String, String)] = plotIds.map{case (name, sheetId) => (name, pubhtml(sheetId))}
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
        key = "11g7db0Ael5BNC7Y7vDaEhod4qyb-0lMt2x4mnJw0lxM",
        gid = 769056270,
        plotIds = Seq(
          "Genderová struktura" -> 1030807690,
          "Věková struktura" -> 237386558,
          "Počet lidí" -> 1215865940,
          "Počet lidí v %" -> 384816554,
          "Časy stanovišť" -> 240092119
        )
      ),
      additionalAlternativeLinks = Seq(
        "PDF" -> "https://drive.google.com/file/d/0B-ovUvdMop8mdmlJb2szazllbE0/view?usp=sharing"
      )
    )
  )

  def forYear(year: Year) = {
    val title = s"Výsledky Z Brna do Brna ${year.year}"
    val plots = CompactPrinter.apply(JsArray(year.dataSource.plots.map{case (x, y) => JsArray(JsString(x), JsString(y))}: _*))
    "<!DOCTYPE html>"+
    <html>
      <head>
        <meta charset="utf-8" />
        <link rel="stylesheet" type="text/css" href="../../statistiky/main.min.css" />
        <script type="text/javascript" src="../../statistiky/main.min.js"></script>
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