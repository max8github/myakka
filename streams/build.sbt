ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"
val AkkaVersion = "2.8.3"
val ScalaTestVersion = "3.2.16"

lazy val root = (project in file("."))
  .settings(
    name := "streams",
    description := "Example sbt project that compiles using Scala 3",
    scalacOptions ++= Seq("-deprecation"),
    idePackagePrefix := Some("org.example.akka.streams"),
    libraryDependencies ++= Seq(
      "org.scalactic" %% "scalactic" % ScalaTestVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
      "ch.qos.logback" % "logback-classic" % "1.4.9"
    )
  )

