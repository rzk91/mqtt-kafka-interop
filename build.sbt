ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "mqtt-kafka-interop",
    libraryDependencies ++= circeDependencies ++ otherDependencies
  )

val circeVersion = "0.14.1"

val circeDependencies = Seq(
  "io.circe" %% "circe-core"           % circeVersion,
  "io.circe" %% "circe-generic"        % circeVersion,
  "io.circe" %% "circe-parser"         % circeVersion,
  "io.circe" %% "circe-optics"         % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion
)

val otherDependencies = Seq(
  "net.sigusr"            %% "fs2-mqtt"               % "1.0.0",
  "com.github.fd4s"       %% "fs2-kafka"              % "3.0.0-M8",
  "com.github.pureconfig" %% "pureconfig"             % "0.17.4",
  "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.17.4",
  "org.typelevel"         %% "log4cats-slf4j"         % "2.6.0",
  "org.slf4j"              % "slf4j-log4j12"          % "2.0.5"
)
