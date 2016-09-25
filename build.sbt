val appVersion= "1.0"

lazy val scalaV = "2.11.8"

val jqueryName: String = "jquery/2.1.4/jquery.js"

val jqPlot = "org.webjars" % "jqplot" % "1.0.8r1250"

val bootstrap = "org.webjars" % "bootstrap" % "3.3.7-1"

import com.typesafe.sbt.web.PathMapping
import com.typesafe.sbt.web.pipeline.Pipeline

val removeLibs = taskKey[Pipeline.Stage]("Removes libraries")

val removeUnversionedAssets = taskKey[Pipeline.Stage]("Removes unversioned assets")

lazy val server = (project in file("server")).settings(
  version := appVersion,
  name := "zbdb-stats-server",
  scalaVersion := scalaV,
  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(concat, removeLibs, filter, digest, simpleUrlUpdate/*, digest*/, removeUnversionedAssets, gzip),
  includeFilter in digest := "*",
  excludeFilter in digest := "*.html",
  includeFilter in filter := "*.less" || "*.note" || "*.source" || "*.css" || "*.js",
  excludeFilter in filter := "main.js" || "main.css",
  resourceGenerators in Assets += Def.task {
    for(year <- PageGenerator.Years) yield {
      val file = (resourceManaged in Assets).value / "assets" / s"${year.year}.html"
      println(s"Writing $file…")
      IO.write(file, PageGenerator.forYear(year))
      file
    }
  }.taskValue,
  removeUnversionedAssets := { mappings: Seq[PathMapping] =>
    mappings.filter{case (file, name) => !name.startsWith("main.")}
  },
  removeLibs := { mappings: Seq[PathMapping] => // Most of libs are already included in a CSS/JS file, so skip them
    mappings.filter{case (file, name) => (!name.startsWith("lib/")) || name.startsWith("lib/bootstrap/fonts")}
  },
  includeFilter in simpleUrlUpdate := "*.css" || "*.js" || "*.html",
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile <<= (compile in Compile) dependsOn scalaJSPipeline,
  Concat.groups := Seq(
    "main.js" -> group(Seq("zbdb-stats-client-jsdeps.min.js", "zbdb-stats-client-opt.js", "zbdb-stats-client-launcher.js"))
  ),
  LessKeys.cleancss := true,
  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.0.0",
    bootstrap,
    jqPlot,
    specs2 % Test
  ),
  // Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
  EclipseKeys.preTasks := Seq(compile in Compile)
).enablePlugins(PlayScala)//.dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  name := "zbdb-stats-client",
  version := appVersion,
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    "com.lihaoyi" %%% "scalatags" % "0.5.2",
    "com.github.marklister" %%% "product-collections" % "1.4.2"
  ),
  jsDependencies ++= Seq(
    bootstrap / "bootstrap.min.js",
    "org.webjars" % "momentjs" % "2.10.6" / "min/moment.min.js",
    "org.webjars" % "moment-timezone" % "0.4.0-1" / "moment-timezone-with-data-2010-2020.js" dependsOn "min/moment.min.js",
    "org.webjars" % "jquery" % "2.1.4" / jqueryName minified "jquery/2.1.4/jquery.min.js",
    ProvidedJS / "jquery.stickytableheaders.js" /*minified "jquery.stickytableheaders.min.js"*/ dependsOn jqueryName,  // Cannot use minified version as it is patched,
    jqPlot / "jquery.jqplot.min.js" dependsOn jqueryName,
    jqPlot / "jqplot.dateAxisRenderer.min.js" dependsOn "jquery.jqplot.min.js",
    jqPlot / "jqplot.bubbleRenderer.min.js" dependsOn "jquery.jqplot.min.js",
    jqPlot / "jqplot.barRenderer.min.js" dependsOn "jquery.jqplot.min.js",
    jqPlot / "jqplot.pointLabels.min.js" dependsOn "jquery.jqplot.min.js",
    jqPlot / "jqplot.highlighter.min.js" dependsOn "jquery.jqplot.min.js",
    "org.webjars.bower" % "console-polyfill" % "0.2.2" / "console-polyfill/0.2.2/index.js"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)//.dependsOn(sharedJs)

/*lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js*/


name := "zbdb-stats"

version := appVersion

// loads the server project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
