import Dependencies._
import ScalaOptions._

organization in ThisBuild := "com.enfore"
ThisBuild / crossScalaVersions := supportedVersions
version in ThisBuild := "unstable-SNAPSHOT"
fork in Test in ThisBuild := true

lazy val http4s = Seq(http4sCore, http4sDsl, http4sCirce, http4sServer)

lazy val commonScalaSettings = Seq(
  scalacOptions ++= compilerFlags,
  Test / fork := true,
  scalaVersion := "2.12.11",
  organization := "com.enfore",
  scalacOptions ++= compilerFlags,
  scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Xfatal-warnings")),
  Compile / doc / javacOptions ++= Seq(
    "-no-link-warnings"
  ),
  Compile / doc / scalacOptions ++= Seq(
    "-no-link-warnings"
  )
)

lazy val sharedDependencies =
  Seq(
    enumaratum,
    enumeratumCirce,
    circeRefined,
    circeCore,
    circeParser,
    circeExtras,
    circeGeneric,
    circeDerivation,
    scalaCompat,
    scalatest % Test
  )

lazy val root = (project in file("."))
  .settings(name := "openapi")
  .settings(publish / skip := true)
  .settings(commonScalaSettings: _*)
  .aggregate(`openapi-scala`, `openapi-lib`, `openapi-http4s-lib`, `sbt-openapi`)

lazy val `openapi-scala` = (project in file("openapi-scala"))
  .settings(
    name := "openapi-scala",
    libraryDependencies ++= Seq(
      circeYaml % "test",
      scalameta % "test"
    ) ++ sharedDependencies
  )
  .settings(commonScalaSettings ++ publishSettings: _*)

lazy val `openapi-lib` = (project in file("openapi-lib"))
  .settings(
    name := "openapi-lib",
    crossScalaVersions := supportedVersions
  )
  .settings(commonScalaSettings ++ publishSettings: _*)

lazy val `openapi-http4s-lib` = (project in file("openapi-http4s-lib"))
  .settings(
    name := "openapi-http4s-lib",
    crossScalaVersions := supportedVersions,
    libraryDependencies ++= sharedDependencies ++ http4s
  )
  .dependsOn(`openapi-lib`)
  .settings(commonScalaSettings ++ publishSettings: _*)

lazy val `sbt-openapi` = (project in file("sbt-openapi"))
  .settings(
    name := "sbt-openapi",
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      Dependencies.scalafmt,
      swaggerCore,
      swaggerParser,
      scriptedPlugin(sbtVersion.value)
    )
  )
  .settings(commonScalaSettings ++ publishSettings: _*)
  .dependsOn(`openapi-scala`)

lazy val publishSettings = Seq(
  crossPaths := false,
  autoAPIMappings := true,
  publishTo := Some(
    Opts.resolver.sonatypeSnapshots
  ),
  useGpg := false,
  usePgpKeyHex("1EAA6358E4812E9E"),
  pgpPublicRing := file(".") / "project" / ".gnupg" / "pubring.gpg",
  pgpSecretRing := file(".") / "project" / ".gnupg" / "secring.gpg",
  pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toArray),
  publishMavenStyle := true,
  homepage := Some(url("https://github.com/NumberFour/openapi-scala")),
  developers := List(
    Developer("ChetanBhasin", "Chetan Bhasin", "chetan.bhasin@numberfour.eu", url("https://github.com/ChetanBhasin")),
    Developer("bijancn", "Bijan Chokoufe Nejad", "bijan.nejad@numberfour.eu", url("https://github.com/bijancn"))
  ),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/NumberFour/openapi-scala"),
      "scm:git:git@github.com:NumberFour/openapi-scala",
      "scm:git:https://github.com/NumberFour/openapi-scala"
    )
  )
)
