import com.enfore.plugin.BuildInfo
import com.enfore.plugin.BasicBuildPlugin._

ThisBuild / organization := BuildInfo.organization
ThisBuild / version := "0.0.7-5-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(name := "openapi")
  .settings(scalaVersion := "2.12.8")
  .aggregate(`openapi-scala`, `openapi-lib`, `sbt-openapi`)
  .enablePlugins(ScalaCrossPlugin, NexusPublishPlugin)

lazy val `openapi-scala` = (project in file("openapi-scala"))
  .settings(
    name := "openapi-scala",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    libraryDependencies ++= Seq(
      "com.github.mpilquist" %% "simulacrum"       % "0.15.0",
      "com.beachape"         %% "enumeratum"       % "1.5.13",
      "io.circe"             %% "circe-yaml"       % "0.9.0",
      "io.circe"             %% "circe-generic"    % "0.10.0",
      "com.beachape"         %% "enumeratum-circe" % "1.5.20",
      "org.scalatest"        %% "scalatest"        % "3.0.5" % "test",
      "org.scalameta"        %% "scalameta"        % "4.1.0" % "test"
    ),
    scalacOptions ++= Seq("-language:implicitConversions"),
    scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Xfatal-warnings")),
    Compile / doc / javacOptions ++= Seq(
      "-no-link-warnings"
    ),
    Compile / doc / scalacOptions ++= Seq(
      "-no-link-warnings"
    )
  )
  .enablePlugins(Scala212Plugin, NexusPublishPlugin, BasicBuildPlugin)

lazy val `openapi-lib` = (project in file("openapi-lib"))
  .settings(
    name := "openapi-lib",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "1.2.0",
    scalacOptions ++= Seq("-language:implicitConversions"),
    scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Xfatal-warnings")),
    Compile / doc / javacOptions ++= Seq(
      "-no-link-warnings"
    ),
    Compile / doc / scalacOptions ++= Seq(
      "-no-link-warnings"
    )
  )
  .enablePlugins(Scala212Plugin, NexusPublishPlugin, BasicBuildPlugin)

lazy val `sbt-openapi` = (project in file("sbt-openapi"))
  .settings(
    name := "sbt-openapi",
    sbtPlugin := true,
    organization := "com.enfore"
  )
  .dependsOn(`openapi-scala`)
  .enablePlugins(Scala212Plugin, SbtPlugin, NexusPublishPlugin, BasicBuildPlugin)

