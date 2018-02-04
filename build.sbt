name := "websocket-pub-sub"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= {
  val akka      = "com.typesafe.akka"
  val akkaV     = "2.5.9"
  val akkaHttpV = "10.1.0-RC1"
  val circe     = "io.circe"
  val circeV    = "0.9.1"
  Seq(
    akka  %% "akka-actor"         % akkaV,
    akka  %% "akka-stream"        % akkaV,
    akka  %% "akka-cluster-tools" % akkaV,
    akka  %% "akka-http"          % akkaHttpV,
    circe %% "circe-core"         % circeV,
    circe %% "circe-generic"      % circeV,
    circe %% "circe-parser"       % circeV
  )
}

scalafmtOnCompile in ThisBuild := true
