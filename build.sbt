import com.enfore.plugin.BuildInfo
import com.enfore.plugin.BasicBuildPlugin._

ThisBuild / organization := BuildInfo.organization
ThisBuild / version := "0.0.6"

lazy val root = (project in file("."))
  .settings(name := "openapi-scala")
  .settings(scalaVersion := "2.12.8")
  .aggregate(`openapi-scala-sbt`, `openapi-sbt-plugin`)
  .enablePlugins(ScalaCrossPlugin, NexusPublishPlugin)

lazy val `openapi-scala-sbt` = (project in file("openapi-scala-sbt"))
  .settings(
    name := "openapi-scala-sbt",
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
  .enablePlugins(Scala212Plugin, SbtPlugin, NexusPublishPlugin, BasicBuildPlugin)

lazy val `openapi-sbt-plugin` = (project in file("openapi-sbt-plugin"))
  .settings(
    name := "openapi-sbt-plugin",
    sbtPlugin := true,
    organization := "com.enfore"
  )
  .dependsOn(`openapi-scala-sbt`)
  .enablePlugins(Scala212Plugin, SbtPlugin, NexusPublishPlugin, BasicBuildPlugin)
