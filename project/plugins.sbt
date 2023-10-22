// Comment to get more information during initialization
logLevel := Level.Warn

// Resolvers
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url("heroku-sbt-plugin-releases",
  url("https://dl.bintray.com/heroku/sbt-plugins/"))(Resolver.ivyStylePatterns)

// Sbt plugins
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.20")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.3")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.2")

addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.2.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")

addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.5.1")

addSbtPlugin("net.ground5hark.sbt" % "sbt-concat" % "0.2.0")

// WARNING: when switching between fork, check the logic of includeFilter and excludeFilter.
// There are multiple forks with a different logic of includeFilter and excludeFilter.
// When you aren't careful, it can lead to missing/extra files in the export.
addSbtPlugin("com.github.karelcemus" % "sbt-filter" % "1.1.0")

addSbtPlugin("org.github.ngbinh" % "sbt-simple-url-update" % "1.0.4")

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta43")

addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.21.1")

dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"

libraryDependencies += "com.lihaoyi" %% "scalatags" % "0.11.1"
