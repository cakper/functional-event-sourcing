name := "functional-event-sourcing"

version := "0.1"

scalaVersion := "2.13.2"

resolvers += Resolver.mavenLocal

val Http4sVersion = "0.21.4"
val circeVersion  = "0.13.0"

libraryDependencies += "io.monix"               %% "monix"                % "3.2.2"
libraryDependencies += "org.scalactic"          %% "scalactic"            % "3.1.2"
libraryDependencies += "org.scalatest"          %% "scalatest"            % "3.1.2" % "test"
libraryDependencies += "com.eventstore"         % "db-client-java"        % "1.0.2-SNAPSHOT"
libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat"   % "0.9.1"
libraryDependencies += "org.mongodb.scala"      %% "mongo-scala-driver"   % "2.9.0"
libraryDependencies += "org.http4s"             %% "http4s-blaze-server"  % Http4sVersion
libraryDependencies += "org.http4s"             %% "http4s-circe"         % Http4sVersion
libraryDependencies += "org.http4s"             %% "http4s-dsl"           % Http4sVersion
libraryDependencies += "ch.qos.logback"         % "logback-classic"       % "1.2.3"
libraryDependencies += "io.circe"               %% "circe-core"           % circeVersion
libraryDependencies += "io.circe"               %% "circe-generic"        % circeVersion
libraryDependencies += "io.circe"               %% "circe-parser"         % circeVersion
libraryDependencies += "io.circe"               %% "circe-generic-extras" % circeVersion
