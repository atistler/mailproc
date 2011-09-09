// set the name of the project
name := "mailproc"

version := "1.0"

organization := "net.logicworks"

scalaVersion := "2.9.0-1"

mainClass := Some("mailproc.MailProc")

resolvers += "Java.net Repo" at "http://download.java.net/maven/2/"

resolvers ++= Seq(
  "snapshots" at "http://scala-tools.org/repo-snapshots",
  "releases" at "http://scala-tools.org/repo-releases",
  // "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Akka Repo" at "http://akka.io/repository"
)

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.5",
  // with Scala 2.8.1
  "org.specs2" %% "specs2-scalaz-core" % "5.1-SNAPSHOT" % "test",
  "se.scalablesolutions.akka" % "akka-actor" % "1.2-RC6"
  // with Scala 2.9.0
  // "org.specs2" %% "specs2-scalaz-core" % "6.0.RC2" % "test"
)

libraryDependencies += "org.freemarker" % "freemarker" % "2.3.18"

libraryDependencies += "org.apache.velocity" % "velocity" % "1.6.2"

libraryDependencies += "org.orbroker" % "orbroker" % "3.1.1"

libraryDependencies += "javax.mail" % "mail" % "1.4.2"

// libraryDependencies += "org.squeryl" %% "squeryl" % "0.9.4"

libraryDependencies += "postgresql" % "postgresql" % "8.4-701.jdbc4"

// libraryDependencies += "postgresql" % "postgresql" % "8.4-701.jdbc4"

retrieveManaged := true

parallelExecution in Test := false

