//import com.typesafe.sbt.SbtAspectj._

name := "akka-sample-twitter-streaming"

version := "1.0"

scalaVersion := "2.11.8"

val kamonVersion = "0.6.0"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-stream" % "2.4.12",
	"com.typesafe.akka" %% "akka-http-core" % "2.4.11",
	"com.typesafe.akka" %% "akka-http-experimental" % "2.4.11",
	"com.hunorkovacs" %% "koauth" % "1.1.0",
	"org.json4s" %% "json4s-native" % "3.3.0",
	"io.kamon" %% "kamon-core" % kamonVersion,
	"io.kamon" %% "kamon-statsd" % kamonVersion,
	"io.kamon" %% "kamon-log-reporter" % kamonVersion,
	"io.kamon" %% "kamon-system-metrics" % kamonVersion,
	"com.typesafe.akka" %% "akka-stream-testkit" % "2.4.12",
	"org.scalatest" %% "scalatest" % "3.0.0" % "test"
)

libraryDependencies += "org.aspectj" % "aspectjweaver" % "1.8.6" withSources()

//aspectjSettings
//
//javaOptions <++= AspectjKeys.weaverOptions in Aspectj
//
//// when you call "sbt run" aspectj weaving kicks in
fork in run := true

kamon.aspectj.sbt.AspectjRunner.testSettings

mainClass in (Compile, run) := Some("TwitterStreamer")
