jarName in assembly := "final.jar"

name := "FacebookSimulator"

version := "0.1"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature", "-deprecation")

parallelExecution in Test := false

resolvers ++= Seq(
	"RoundEights" at "http://maven.spikemark.net/roundeights",
 	"spray repo" at "http://repo.spray.io"
)

val vakka = "2.4.0"
val vspray = "1.3.3"
val vjson4s = "3.2.11"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % vakka,
    "com.typesafe.akka" %% "akka-remote" % vakka,
    "com.typesafe.akka" %% "akka-testkit" % vakka,
    "com.typesafe" % "config" % "1.3.0",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "com.roundeights" %% "hasher" % "1.2.0",
    "org.mindrot" % "jbcrypt" % "0.3m",
    "io.spray" %% "spray-can" % vspray,
    "io.spray" %% "spray-caching" % vspray,
    "io.spray" %% "spray-routing" % vspray,
    "io.spray" %% "spray-client" % vspray,
    "io.spray" %% "spray-testkit" % vspray % "test",
    "org.specs2" %% "specs2" % "2.3.13" % "test",
    "org.json4s" %% "json4s-native" % vjson4s,
    "org.json4s" %% "json4s-ext" % vjson4s
)
