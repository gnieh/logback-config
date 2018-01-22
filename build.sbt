name := "logback-config"

organization := "org.gnieh"

version := "0.1.0-SNAPSHOT"

licenses += ("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://github.com/gnieh/logback-config"))

libraryDependencies += "ch.qos.logback"  % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe"  % "config" % "1.3.2"
libraryDependencies += "junit" % "junit" % "4.12" % Test
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test

// publish settings
publishMavenStyle := true

publishArtifact in Test := false

// do not happen the scala version
crossPaths := false

// exclude scala library, this is a pure java project
autoScalaLibrary := false

// The Nexus repo we're publishing to.
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging)

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

