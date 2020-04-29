import com.enfore.plugin.BuildInfo
import com.enfore.plugin.BasicBuildPlugin._

ThisBuild / organization := BuildInfo.organization
ThisBuild / scalaVersion := Scala213Plugin.scala213Version
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

lazy val scalaMacros: Seq[Def.Setting[_]] = Seq(
  libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) => Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
    case _             => Seq()
  }),
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) => Seq()
    case _             => Seq("-Ymacro-annotations")
  })
)

lazy val scalaCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2"
lazy val sharedDependencies = Seq(
  "com.beachape" %% "enumeratum"       % "1.5.13",
  "com.beachape" %% "enumeratum-circe" % "1.5.22",
  "io.circe"     %% "circe-refined"    % Circe.latestDependencies.head.revision,
  scalaCompat
) ++ Circe.latestDependencies ++ ScalaTest.latestDependencies

lazy val root = (project in file("."))
  .settings(name := "openapi")
  .aggregate(`openapi-scala`, `openapi-lib`, `openapi-http4s-lib`, `sbt-openapi`)
  .enablePlugins(ScalaCrossPlugin, NexusPublishPlugin)

lazy val `openapi-scala` = (project in file("openapi-scala"))
  .settings(
    name := "openapi-scala",
    scalaMacros,
    libraryDependencies ++= Seq(
      "io.circe"      %% "circe-yaml" % "0.11.0-M1" % "test",
      "org.scalameta" %% "scalameta"  % "4.3.0"     % "test"
    ) ++ sharedDependencies
  )
  .settings(commonScalaSettings)
  .enablePlugins(Scala212Plugin, NexusPublishPlugin, BasicBuildPlugin)

lazy val `openapi-lib` = (project in file("openapi-lib"))
  .settings(
    name := "openapi-lib"
  )
  .settings(commonScalaSettings)
  .enablePlugins(ScalaCrossPlugin, NexusPublishPlugin, BasicBuildPlugin)

lazy val `openapi-http4s-lib` = (project in file("openapi-http4s-lib"))
  .settings(
    name := "openapi-http4s-lib",
    scalaMacros,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-derivation" % "0.12.0-M7"
    ) ++ sharedDependencies
      ++ RefinedTypes.latestDependencies
      ++ Http4sCirce.latestDependencies
  )
  .dependsOn(`openapi-lib`)
  .settings(commonScalaSettings)
  .enablePlugins(ScalaCrossPlugin, NexusPublishPlugin, BasicBuildPlugin)

lazy val `sbt-openapi` = (project in file("sbt-openapi"))
  .settings(
    name := "sbt-openapi",
    sbtPlugin := true,
    organization := "com.enfore",
    libraryDependencies ++= Seq(
      "io.swagger.core.v3"   % "swagger-core"   % "2.0.9",
      "io.swagger.parser.v3" % "swagger-parser" % "2.0.14"
    ),
    addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, organization),
    buildInfoPackage := organization.value + ".openapi.plugin"
  )
  .settings(commonScalaSettings)
  .dependsOn(`openapi-scala`)
  .enablePlugins(Scala212Plugin, SbtPlugin, NexusPublishPlugin, BasicBuildPlugin, BuildInfoPlugin)
