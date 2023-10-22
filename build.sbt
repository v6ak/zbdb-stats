import scala.sys.process._
import java.io.IOException

val appVersion= "1.0"

lazy val scalaV2 = "2.13.12"

lazy val scalaV3 = "3.3.1"

val bootstrapVersion = "5.3.2"

import com.typesafe.sbt.web.PathMapping
import com.typesafe.sbt.web.pipeline.Pipeline

val removeLibs = taskKey[Pipeline.Stage]("Removes libraries")

val moveLibs = taskKey[Pipeline.Stage]("Moves libraries")

val removeUnversionedAssets = taskKey[Pipeline.Stage]("Removes unversioned assets")

val explicitlyExcludedLibFiles = Set(
  "lib/bootstrap/fonts/glyphicons-halflings-regular.eot", "lib/bootstrap/fonts/glyphicons-halflings-regular.svg",
  "lib/bootstrap/fonts/glyphicons-halflings-regular.ttf", "lib/bootstrap/fonts/glyphicons-halflings-regular.woff2"
)

val genHtmlDir =  settingKey[File]("Output directory for HTML generated files")

val PublicDirName = "statistiky" // TODO: DRY this constant from routes

val YearDir = "^[0-9]+(?:$|/.*)".r

def write(file: File, content: String) = {
  println(s"Writing $file…")
  IO.write(file, content)
  file
}

def download(out: File, source: URL) = {
  if(out.exists) {
    println(s"Source $source is already downloaded at $out.")
  } else {
    val tmpFile = file(out + ".tmp")
    println(s"Downloading $source…")
    tmpFile.getParentFile.mkdirs()
    source #> tmpFile !;
    if(! tmpFile.renameTo(out)) {
      throw new IOException(s"Renaming $tmpFile to $out failed!")
    }
    println(s"Downloaded $source as $out")
  }
  out
}

// Generates other assets than client JS, plus contains a server for development purposes
lazy val server = (project in file("server")).settings(
  version := appVersion,
  name := "zbdb-stats-server",
  scalacOptions ++= Seq("-deprecation", "-feature"),
  scalaVersion := scalaV2,
  scalaJSProjects := Seq(client),
  scalaJSStage := FullOptStage,
  Assets / pipelineStages := Seq(scalaJSPipeline),
  pipelineStages := Seq(concat, removeLibs, filter, digest, simpleUrlUpdate/*, digest*/, removeUnversionedAssets, moveLibs),
  digest / includeFilter := "*",
  digest / excludeFilter := "*.html" || "*.csv" || "*.json" || "*.json.new" ||
    // When sbt-simple-url-update updates path for glyphicons-halflings-regular.woff, it garbles the path for glyphicons-halflings-regular.woff2.
    "glyphicons-halflings-regular.woff",
  filter / excludeFilter := "*.scss" || "*.note" || "*.source" || "*.css" - "main.css" || "*.js" - "main.min.js",
  filter / includeFilter := "*.css" || "*.html" || "*.js" || "*.csv" || "*.svg" || "*.woff" || "*.ttf" || "*.eot" || "*.woff2" || "*.json.new",
  genHtmlDir := target.value / "web" / "html" / "main",
  Assets / resourceDirectories += genHtmlDir.value,
  Assets / resourceGenerators += Def.task {
    val yearHtmlFiles = for(year <- PageGenerator.Years) yield {
      write(
        file = genHtmlDir.value / s"${year.year}" / PublicDirName / s"index.html",
        content = PageGenerator.forYear(year, PublicDirName)
      )
    }
    val allYearsListJsonFile = write(genHtmlDir.value / "years.json.new", PageGenerator.allYearsJsonString)
    yearHtmlFiles :+ allYearsListJsonFile
  }.taskValue,
  Assets / resourceGenerators += Def.task {
    for(year <- PageGenerator.Years if year.dataSource.csvDownloadUrl startsWith "https://") yield {
      download(
        out = genHtmlDir.value / s"${year.year}" / PublicDirName / s"${year.year}.csv",
        source = url(year.dataSource.csvDownloadUrl)
      )
    }
  }.taskValue,
  removeUnversionedAssets := { mappings: Seq[PathMapping] =>
    mappings.filter{case (file, name) => !(name.startsWith("main.") || explicitlyExcludedLibFiles.contains(name))}
  },
  removeLibs := { mappings: Seq[PathMapping] => // Most of libs are already included in a CSS/JS file, so skip them
    mappings.filter{case (file, name) => (!name.startsWith("lib/")) || name.startsWith("lib/bootstrap/fonts")}
  },
  moveLibs := { mappings: Seq[PathMapping] =>
    mappings.map {
      case other @ (_, YearDir()) => other
      case (file, name) => (file, PublicDirName + "/" + name)
    }
  },
  simpleUrlUpdate / includeFilter := "*.css" || "*.js" || "*.html",
  // triggers scalaJSPipeline when using compile or continuous compilation
  Compile / compile := ((Compile / compile) dependsOn scalaJSPipeline).value,
  Concat.groups := Seq("main.min.js" -> group(Seq("zbdb-stats-client-opt-bundle.js"))),
  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.2.0",
    guice,
    "org.webjars" % "bootstrap" % bootstrapVersion,
    specs2 % Test
  ),
).enablePlugins(PlayScala, SbtWeb, WebScalaJSBundlerPlugin)//.dependsOn(sharedJvm)

// Generates client JS; other assets are generated by the server subproject
lazy val client = (project in file("client")).settings(
  name := "zbdb-stats-client",
  version := appVersion,
  scalacOptions ++= Seq("-deprecation", "-feature"),
  scalaJSStage := FullOptStage,
  scalaVersion := scalaV3,
  scalaJSUseMainModuleInitializer := true,
  Test / scalaJSUseMainModuleInitializer := false,
  webpack / version := "5.88.2", // https://github.com/ScalablyTyped/Converter/issues/546
  webpackConfigFile := Some(baseDirectory.value / "custom.webpack.config.js"),

  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "2.7.0",
    "com.lihaoyi" %%% "scalatags" % "0.12.0",
  ),
  Compile / npmDependencies ++= Seq(
    "comma-separated-values" -> "3.6.4",
    "bootstrap" -> bootstrapVersion,
    "moment" -> "2.10.6",
    "moment-timezone" -> "0.4.0",
    "chart.js" -> "4.4.0",
    "chartjs-adapter-moment" -> "1.0.1",
  ),
  stIgnore ++= List("moment", "moment-timezone", "bootstrap", "chart.js", "chartjs-adapter-moment",
    "comma-separated-values"),
).enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin, ScalablyTypedConverterPlugin)//.dependsOn(sharedJs)

/*lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js*/


name := "zbdb-stats"

version := appVersion

