name := """ScalaPlay"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(guice,
    "com.typesafe.akka" %% "akka-stream-typed" % "2.6.10",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,

    "org.sangria-graphql" %% "sangria-spray-json" % "1.0.2",
    "org.sangria-graphql" %% "sangria" % "2.0.1",
    "org.sangria-graphql" %% "sangria-play-json" % "2.0.1",
    "org.sangria-graphql" %% "sangria-akka-streams" % "1.0.2",
    "io.monix" %% "monix" % "3.3.0",

    "com.typesafe.slick" %% "slick" % "3.3.3",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
    "org.postgresql" % "postgresql" % "42.2.18"
)
