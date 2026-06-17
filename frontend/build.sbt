enablePlugins(ScalaJSPlugin)

ThisBuild / scalaVersion := "3.3.5"

name := "quebec-urban-pulse-frontend"

scalaJSUseMainModuleInitializer := true

libraryDependencies += "com.raquo" %%% "laminar" % "17.2.1"
