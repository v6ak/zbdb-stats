enablePlugins(ScalaJSPlugin)

name := """zbdb-stats"""

version := "1.0"

scalaVersion := "2.11.7"

// Change this to another test framework if you prefer
//libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0"

libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.5.2"

libraryDependencies += "com.github.marklister" %%% "product-collections" % "1.4.2"

//libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.3.6"

jsDependencies += "org.webjars" % "momentjs" % "2.10.6" / "min/moment.min.js"

jsDependencies += "org.webjars" % "moment-timezone" % "0.4.0-1" / "moment-timezone-with-data-2010-2020.js" dependsOn "min/moment.min.js"

val jqueryName: String = "jquery/2.1.4/jquery.js"

jsDependencies += "org.webjars" % "jquery" % "2.1.4" / jqueryName minified "jquery/2.1.4/jquery.min.js"

//jsDependencies += "org.webjars" % "floatThead" % "1.2.12" / "jquery.floatThead.min.js" dependsOn "jquery.min.js"

//"org.webjars.bower" % "StickyTableHeaders" % "0.1.17"
jsDependencies += ProvidedJS / "jquery.stickytableheaders.js" /*minified "jquery.stickytableheaders.min.js"*/ dependsOn jqueryName  // Cannot use minified version as it is patched

val jqPlot = "org.webjars" % "jqplot" % "1.0.8r1250"

jsDependencies += jqPlot / "jquery.jqplot.min.js" dependsOn jqueryName

jsDependencies += jqPlot / "jqplot.dateAxisRenderer.min.js" dependsOn "jquery.jqplot.min.js"

jsDependencies += jqPlot / "jqplot.bubbleRenderer.min.js" dependsOn "jquery.jqplot.min.js"

jsDependencies += jqPlot / "jqplot.barRenderer.min.js" dependsOn "jquery.jqplot.min.js"

jsDependencies += jqPlot / "jqplot.pointLabels.min.js" dependsOn "jquery.jqplot.min.js"

jsDependencies += jqPlot / "jqplot.highlighter.min.js" dependsOn "jquery.jqplot.min.js"

// jsDependencies += ProvidedJS / "libs/sticky-table/src/sticky.js"

jsDependencies += "org.webjars.bower" % "console-polyfill" % "0.2.2" / "console-polyfill/0.2.2/index.js"

persistLauncher in Compile := true

persistLauncher in Test := false
