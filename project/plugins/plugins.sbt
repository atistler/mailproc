resolvers += "retronym-releases" at "http://retronym.github.com/repo/releases"

resolvers += "retronym-snapshots" at "http://retronym.github.com/repo/snapshots"

resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"

libraryDependencies += "com.github.retronym" %% "sbt-onejar" % "0.3-SNAPSHOT"

libraryDependencies += "com.github.mpeltonen" %% "sbt-idea" % "0.10.0"
