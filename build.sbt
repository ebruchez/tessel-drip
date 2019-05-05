enablePlugins(ScalaJSBundlerPlugin)
enablePlugins(BuildInfoPlugin)

name := "tessel-drip"

version := "1.0-SNAPSHOT"

version in webpack := "4.8.1"
version in startWebpackDevServer := "3.1.4"

scalaVersion := "2.11.8"

libraryDependencies        += "org.scala-js"           %%% "scalajs-dom"     % "0.9.0"
libraryDependencies        += "com.nativelibs4java"    %%  "scalaxy-streams" % "0.3.4" % "provided"
libraryDependencies        += "org.scala-lang.modules" %%  "scala-async"     % "0.9.5" % "provided"

npmDependencies in Compile += "node-fetch"    → "1.6.3"
npmDependencies in Compile += "node-schedule" → "1.1.0"
npmDependencies in Compile += "relay-mono"    → "0.1.3"

scalaJSModuleKind          := ModuleKind.CommonJSModule
webpackConfigFile          := Some(baseDirectory.value / "tessel.webpack.config.js")

scalaJSUseMainModuleInitializer := true

def getApiKey(name: String) = Option(System.getenv(name)) getOrElse (throw new RuntimeException(s"missing environment variable $name"))

buildInfoKeys              := Nil
buildInfoKeys              += BuildInfoKey.action("IFTTTKey")(getApiKey("TESSEL_IFTTT_KEY"))
buildInfoKeys              += BuildInfoKey.action("DarkSkyKey")(getApiKey("TESSEL_DARK_SKY_KEY"))
buildInfoKeys              += BuildInfoKey.action("WeatherUndergroundKey")(getApiKey("TESSEL_WEATHER_UNDERGROUND_KEY"))
buildInfoPackage           := "org.bruchez.tessel"
buildInfoObject            := "APIKeys"