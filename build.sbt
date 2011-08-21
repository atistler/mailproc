// set the name of the project
name := "mailproc"

version := "1.0"

organization := "net.logicworks"

scalaVersion := "2.9.0-1"

resolvers += "Java.net Repo" at "http://download.java.net/maven/2/"

libraryDependencies += "org.orbroker" % "orbroker" % "3.1.1"

libraryDependencies += "javax.mail" % "mail" % "1.4.2" 

libraryDependencies += "org.squeryl" %% "squeryl" % "0.9.4"

libraryDependencies += "postgresql" % "postgresql" % "8.4-701.jdbc4"
    
// libraryDependencies += "postgresql" % "postgresql" % "8.4-701.jdbc4"

retrieveManaged := true

