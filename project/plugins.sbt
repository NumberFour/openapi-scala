// Code auto-formatting with compile
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.0")

// Code style hints
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// Build Plugin
addSbtPlugin("com.enfore" % "sbt-n4build" % "1.0.3")

// Code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0")

// Dependency graph generation
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")

addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.3.2")
