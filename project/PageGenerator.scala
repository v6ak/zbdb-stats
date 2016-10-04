object PageGenerator{

  sealed abstract class DataSource{
    def csvAjaxUrl: String
    def csvDownloadUrl: String
    def originalLink: String
    def prefetchAjax: Boolean
  }
  trait CorsAnywhereDataSource extends DataSource {
    override def csvAjaxUrl: String = s"https://cors-anywhere.herokuapp.com/$csvDownloadUrl"
    override def prefetchAjax: Boolean = false
  }
  final case class GoogleSpreadsheetDataSource(key: String) extends DataSource with CorsAnywhereDataSource{
    override def csvDownloadUrl: String = s"https://docs.google.com/spreadsheets/d/$key/pub?gid=0&single=true&output=csv"
    override def originalLink: String = s"https://docs.google.com/spreadsheets/d/$key/pubhtml"
    //override def csvAjaxUrl: String = s"https://zbdb.v6ak.com/spreadsheets/$key"
  }
  final case class FileDataSource(file: String, originalLink: String) extends DataSource{
    override def csvAjaxUrl: String = file
    override def csvDownloadUrl: String = file
    override def prefetchAjax: Boolean = true
  }

  case class Year(year: Int, formatVersion: Int, dataSource: DataSource, startTime: String, endTime: String)



  val Years = Seq(
    Year(
      year = 2015, formatVersion = 2015,
      startTime = "2015-09-18 17:30", endTime = "2015-09-19 19:00",
      dataSource = FileDataSource("zbdb-2015-simplified.csv", "https://docs.google.com/spreadsheets/d/1sCOP6tEAQmjkdRhfrgOd0RLXAlgZ_0NiXAU1prcXqP0/pubhtml?gid=1935861499&single=true")
    ),
    Year(
      year = 2016, formatVersion = 2016,
      startTime = "2016-09-16 17:30", endTime="2016-09-17 20:00",
      dataSource = GoogleSpreadsheetDataSource("1eyNWmTn83l46VucS4DHoMlUxP6zAG8AuMHFzbFBrvpM")
    )
  )

  def forYear(year: Year) = {
    val title = s"Výsledky Z Brna do Brna ${year.year}"
    "<!DOCTYPE html>"+
    <html>
      <head>
        <meta charset="utf-8" />
        <link rel="stylesheet" type="text/css" href="main.min.css" />
        <script type="text/javascript" src="main.min.js"></script>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        {if(year.dataSource.prefetchAjax) <link rel="prefetch" href={year.dataSource.csvAjaxUrl} /> else ""}
        <meta http-equiv="X-UA-Compatible" content="IE=10; IE=9; IE=8; IE=7; IE=EDGE" />
        <title>{title}</title>
      </head>
      <body data-file={year.dataSource.csvAjaxUrl} data-start-time={year.startTime} data-end-time={year.endTime} data-timezone="Europe/Prague" data-max-hour-delta="6" data-format-version={year.formatVersion.toString}>
        <div class="container">
          <h1>{title}</h1>
          <p class="hidden-print">Alternativní podoby: <a href={year.dataSource.originalLink}>Tabulka Google</a>, <a href={year.dataSource.csvDownloadUrl}>CSV</a></p>
        </div>
        <div id="content"><div id="loading-indicator">Načítám výsledky…</div></div>
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