// Code auto-formatting with compile
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.6")

// Code style hints
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// Code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

// SBT Build Info
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

// SBT Sonatype Plugin
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.2")

// SBT PGP Plugin — for package signing
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")

// Automate relase using environment variables
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.0")
