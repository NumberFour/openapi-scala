import com.enfore.plugin.BuildInfo

ThisBuild / organization := BuildInfo.organization

lazy val root = (project in file("."))
  .settings(name := "openapi-scala")
  .settings(scalaVersion := "2.12.8")

lazy val `openapi-scala-sbt` = (project in file("openapi-scala-sbt"))
  .settings(
    name := "openapi-scala-sbt",
    scalacOptions in(Compile, console) ~= (_ filterNot (_ == "-Xfatal-warnings")),
    Compile / doc / javacOptions ++= Seq(
      "-no-link-warnings"
    ),
    Compile / doc / scalacOptions ++= Seq(
      "-no-link-warnings"
    )
  )
  .enablePlugins(Scala212Plugin, SbtPlugin)