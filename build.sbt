// set the name of the project
name := "mailproc"

version := "0.1"

organization := "logicops"

scalaVersion := "2.9.1"

mainClass := Some("logicops.mailproc.MailProc")

scalacOptions ++= Seq("-deprecation","-unchecked")

resolvers ++= Seq(
  "Java.net Repo" at "http://download.java.net/maven/2/",
  "snapshots" at "http://scala-tools.org/repo-snapshots",
  "releases" at "http://scala-tools.org/repo-releases",
  // "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Akka Repo" at "http://akka.io/repository",
  "FuseSource Snapshot Repository" at "http://repo.fusesource.com/nexus/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
  // "org.fusesource.scalate" % "scalate-core" % "1.5.1",
  "commons-lang" % "commons-lang" % "2.6",
  // with Scala 2.8.1
  // "org.specs2" %% "specs2" % "1.5",
  // "org.specs2" %% "specs2-scalaz-core" % "5.1-SNAPSHOT" % "test",
  "org.specs2" %% "specs2" % "1.6.1",
  "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test",
  "se.scalablesolutions.akka" % "akka-actor" % "1.2",
  "org.jsoup" % "jsoup" % "1.6.1",
  "org.freemarker" % "freemarker" % "2.3.18",
  "org.apache.velocity" % "velocity" % "1.6.2",
  "org.orbroker" % "orbroker" % "3.1.1",
  "javax.mail" % "mail" % "1.4.2",
  "postgresql" % "postgresql" % "8.4-701.jdbc4",
  "ch.qos.logback" % "logback-classic" % "0.9.28" % "runtime",
  "commons-io" % "commons-io" % "2.0.1",
  "org.scalaj" %% "scalaj-collection" % "1.2"
)

retrieveManaged := true

seq(oneJarSettings: _*)

parallelExecution in Test := false

