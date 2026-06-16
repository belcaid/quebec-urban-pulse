ThisBuild / scalaVersion := "3.3.5"

name := "quebec-urban-pulse-etl"

libraryDependencies ++= Seq(
  "com.github.tototoshi" %% "scala-csv" % "2.0.0",
  "org.xerial" % "sqlite-jdbc" % "3.47.2.0"
)
