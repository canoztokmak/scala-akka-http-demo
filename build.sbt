name := "movie-reservation"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.5"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.5"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0"

libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % "10.0.7" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
