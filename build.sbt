name := "logback-config"

organization := "org.gnieh"

version := "0.1.0-SNAPSHOT"

licenses += ("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://github.com/gnieh/logback-config"))

libraryDependencies += "ch.qos.logback"  % "logback-classic" % "1.1.7"
libraryDependencies += "com.typesafe"  % "config" % "1.3.0"

// publish settings
publishMavenStyle := true

publishArtifact in Test := false

// do not happen the scala version
crossPaths := false

// exclude scala library, this is a pure java project
autoScalaLibrary := false

// The Nexus repo we're publishing to.
publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { x => false }

pomExtra :=
  <scm>
    <url>https://github.com/gnieh/logback-config</url>
    <connection>scm:git:git://github.com/gnieh/logback-config.git</connection>
    <developerConnection>scm:git:git@github.com:gnieh/logback-config.git</developerConnection>
    <tag>HEAD</tag>
  </scm>
  <developers>
    <developer>
      <id>satabin</id>
      <name>Lucas Satabin</name>
      <email>lucas.satabin@gnieh.org</email>
    </developer>
  </developers>
  <issueManagement>
    <system>github</system>
    <url>https://github.com/gnieh/logback-config/issues</url>
  </issueManagement>

