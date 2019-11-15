name := "scala-influxdb-client"

organization := "io.razem"

scalaVersion := "2.13.1"
crossScalaVersions := Seq(scalaVersion.value, "2.12.8", "2.11.12", "2.10.7")

testOptions in Test += Tests.Argument("-oDF")

useGpg := true
releaseCrossBuild := true

libraryDependencies += "org.asynchttpclient" % "async-http-client" % "2.10.4"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.5"
libraryDependencies += "com.github.tomakehurst" % "wiremock" % "2.25.1" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
  pushChanges
)
