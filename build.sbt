name := """sloth-bike"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.6"

crossScalaVersions := Seq("2.11.12", "2.12.6")

scalacOptions += "-Ypartial-unification"
libraryDependencies += "org.typelevel" %% "cats-core" % "1.1.0"
libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += "com.h2database" % "h2" % "1.4.197"
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.34"
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.3m"
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0"
)

libraryDependencies ++= Seq(
  "org.apache.poi" % "poi" % "3.17",
  "org.apache.poi" % "poi" % "3.17",
  "org.apache.poi" % "poi-ooxml" % "3.17",
  "org.apache.poi" % "poi-ooxml-schemas" % "3.17"
)

libraryDependencies += "net.liftweb" %% "lift-json" % "3.3.0"

libraryDependencies += "io.igl" %% "jwt" % "1.2.2"

libraryDependencies += ws

routesImport += "binders.Binders._"