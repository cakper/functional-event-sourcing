name := "functional-event-sourcing"

version := "0.1"

scalaVersion := "2.13.2"

resolvers += Resolver.mavenLocal

libraryDependencies += "io.monix"               %% "monix"              % "3.2.2"
libraryDependencies += "org.scalactic"          %% "scalactic"          % "3.1.2"
libraryDependencies += "org.scalatest"          %% "scalatest"          % "3.1.2" % "test"
libraryDependencies += "com.eventstore"         % "db-client-java"      % "1.0-SNAPSHOT"
libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1"
libraryDependencies += "org.mongodb.scala"      %% "mongo-scala-driver" % "2.9.0"

val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
