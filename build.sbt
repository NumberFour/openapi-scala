import com.enfore.plugin.BuildInfo
import com.enfore.plugin.BasicBuildPlugin._

ThisBuild / organization := BuildInfo.organization
ThisBuild / version := "unstable-SNAPSHOT"

lazy val commonScalaSettings = Seq(
  scalacOptions ++= Seq("-language:implicitConversions"),
  scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Xfatal-warnings")),
  Compile / doc / javacOptions ++= Seq(
    "-no-link-warnings"
  ),
  Compile / doc / scalacOptions ++= Seq(
    "-no-link-warnings"
  )
)

lazy val root = (project in file("."))
  .settings(name := "openapi")
  .settings(scalaVersion := "2.12.8")
  .aggregate(`openapi-scala`, `openapi-lib`, `openapi-http4s-lib`, `sbt-openapi`)
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
    )
  )
  .settings(commonScalaSettings)
  .enablePlugins(Scala212Plugin, NexusPublishPlugin, BasicBuildPlugin)

lazy val `openapi-lib` = (project in file("openapi-lib"))
  .settings(
    name := "openapi-lib"
  )
  .settings(commonScalaSettings)
  .enablePlugins(Scala212Plugin, NexusPublishPlugin, BasicBuildPlugin)

lazy val `openapi-http4s-lib` = (project in file("openapi-http4s-lib"))
  .settings(
    name := "openapi-http4s-lib",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    libraryDependencies ++= Seq(
      "com.beachape"         %% "enumeratum"       % "1.5.13",
      "com.beachape"         %% "enumeratum-circe" % "1.5.20",
      "io.circe"             %% "circe-derivation" % "0.11.0-M1",
      "io.circe"             %% s"circe-generic"   % "0.11.0",
      "io.circe"             %% s"circe-refined"   % "0.11.0",
      "com.chuusai"          %% "shapeless"        % "2.3.3",
      "com.github.mpilquist" %% "simulacrum"       % "0.15.0",
      "eu.timepit"           %% "refined"          % "0.9.5"
    )
      ++ Http4sCirce.latestDependencies
  )
  .dependsOn(`openapi-lib`)
  .settings(commonScalaSettings)
  .enablePlugins(Scala212Plugin, NexusPublishPlugin, BasicBuildPlugin)

lazy val `sbt-openapi` = (project in file("sbt-openapi"))
  .settings(
    name := "sbt-openapi",
    sbtPlugin := true,
    organization := "com.enfore",
    addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, organization),
    buildInfoPackage := organization.value + ".openapi.plugin"
  )
  .settings(commonScalaSettings)
  .dependsOn(`openapi-scala`)
  .enablePlugins(Scala212Plugin, SbtPlugin, NexusPublishPlugin, BasicBuildPlugin, BuildInfoPlugin)
