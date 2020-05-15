import sbt._

object Dependencies {
  lazy val http4sVersion = "0.21.0-M5"

  lazy val scalatest = "org.scalatest" %% "scalatest"        % "3.1.1"
  lazy val scalameta = "org.scalameta" %% "scalameta"        % "4.3.0"
  lazy val scalafmt  = "org.scalameta" %% "scalafmt-dynamic" % "2.5.0-RC2"

  lazy val enumaratum      = "com.beachape"           %% "enumeratum"              % "1.5.13"
  lazy val enumeratumCirce = "com.beachape"           %% "enumeratum-circe"        % "1.5.22"
  lazy val circeRefined    = "io.circe"               %% "circe-refined"           % "0.13.0"
  lazy val circeCore       = "io.circe"               %% "circe-core"              % "0.13.0"
  lazy val circeParser     = "io.circe"               %% "circe-parser"            % "0.13.0"
  lazy val circeGeneric    = "io.circe"               %% "circe-generic"           % "0.13.0"
  lazy val circeExtras     = "io.circe"               %% "circe-generic-extras"    % "0.13.0"
  lazy val circeYaml       = "io.circe"               %% "circe-yaml"              % "0.13.0"
  lazy val circeDerivation = "io.circe"               %% "circe-derivation"        % "0.13.0-M4"
  lazy val scalaCompat     = "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2"

  lazy val http4sCore   = "org.http4s" %% "http4s-core"   % http4sVersion
  lazy val http4sDsl    = "org.http4s" %% "http4s-dsl"    % http4sVersion
  lazy val http4sCirce  = "org.http4s" %% "http4s-circe"  % http4sVersion
  lazy val http4sServer = "org.http4s" %% "http4s-server" % http4sVersion

  lazy val swaggerCore   = "io.swagger.core.v3"   % "swagger-core"   % "2.0.9"
  lazy val swaggerParser = "io.swagger.parser.v3" % "swagger-parser" % "2.0.14"

  def scriptedPlugin(libVersion: String) = "org.scala-sbt" %% "scripted-plugin" % libVersion

}
