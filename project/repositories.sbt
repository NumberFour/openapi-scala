ThisBuild / resolvers := Seq(
  ("n4-public" at "https://nexus3.internal.numberfour.eu/repository/maven-public/"),
  (Resolver
    .url("n4-public-ivy", new java.net.URL("https://nexus3.internal.numberfour.eu/repository/maven-public/"))(
      Patterns(
        "[organization]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]")))
)
