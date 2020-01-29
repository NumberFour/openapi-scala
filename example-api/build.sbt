ThisBuild / resolvers := Seq(
  ("n4-public" at "http://nexus-aws.corp.numberfour.eu/nexus/content/repositories/public/")
    .withAllowInsecureProtocol(true),
  (Resolver
    .url(
      "n4-public-ivy",
      new java.net.URL("http://nexus-aws.corp.numberfour.eu/nexus/content/repositories/public/"))(Patterns(
      "[organization]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]")))
    .withAllowInsecureProtocol(true)
)

def enfore(project: String): ModuleID  = "com.enfore" %% project             % "1.3.0"
lazy val sharedOAPIObjects             = "com.enfore" %% "shared-apis"       % "1.0.0"

def refined(subproject: String): ModuleID = refined.organization %% s"refined-$subproject" % refined.revision
lazy val refined                          = "eu.timepit"         %% "refined"              % "0.9.4"

lazy val root = (project in file("."))
  .settings(name := "api-example")
  .aggregate(`api-example`)

lazy val api = (project in file("api"))
  .settings(
    name := "api",
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      sharedOAPIObjects
    )
  )
  .settings(
    openAPISource := file("api") / "src" / "main" / "openapi",
    openAPISourceFile := file("api") / "src" / "main" / "openapi" / "main.yaml",
    openAPIOutputPackage := "com.enfore.openapi.example",
    extraSourcesJar := "shared-apis",
    sourceGenerators in Compile += openAPIBuild
  )
  .settings(
    libraryDependencies += "com.enfore" %% "openapi-lib"        % "1.2.0",
    libraryDependencies += "com.enfore" %% "openapi-http4s-lib" % "1.2.0",
  )
  .enablePlugins(OpenAPIPlugin)


lazy val `api-example` = (project in file("api-example"))
  .settings(
    name := "api-example",
    Compile / doc / javacOptions ++= Seq(
      "-no-link-warnings"
    ),
    Compile / doc / scalacOptions ++= Seq(
      "-no-link-warnings"
    ),
    libraryDependencies ++= Seq(
      refined,
      enfore("http4s"),
    ),
    mainClass in (Compile, run) := Some("com.enfore.apis.example.server.Main"),
  )
  .dependsOn(api)
  .aggregate(api)
