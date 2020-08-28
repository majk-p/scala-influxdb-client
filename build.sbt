name := "scala-influxdb-client"

organization := "io.razem"

scalaVersion := "2.12.10"
crossScalaVersions := Seq(scalaVersion.value, "2.13.1", "2.11.12")

publishTo := sonatypePublishToBundle.value

testOptions in Test += Tests.Argument("-oDF")

releaseCrossBuild := true

libraryDependencies += "org.asynchttpclient" % "async-http-client" % "2.10.4"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.5"
libraryDependencies += "com.github.tomakehurst" % "wiremock" % "2.25.1" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"
libraryDependencies += "com.dimafeng" %% "testcontainers-scala" % "0.33.0" % "test"
libraryDependencies += "com.softwaremill.sttp.client" %% "core" % "2.2.6"
libraryDependencies += "org.typelevel" %% "cats-core" % "2.1.1"
libraryDependencies += "org.typelevel" %% "cats-effect" % "2.1.4"
libraryDependencies += "com.softwaremill.sttp.client" %% "okhttp-backend" % "2.2.6"

scalacOptions += "-Ypartial-unification"
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

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
