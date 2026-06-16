ThisBuild / scalaVersion := "3.3.5"

name := "quebec-urban-pulse-backend"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.7",
  "org.http4s" %% "http4s-ember-server" % "0.23.30",
  "org.http4s" %% "http4s-dsl" % "0.23.30",
  "org.http4s" %% "http4s-circe" % "0.23.30",
  "io.circe" %% "circe-generic" % "0.14.10",
  "org.xerial" % "sqlite-jdbc" % "3.47.2.0"
)
