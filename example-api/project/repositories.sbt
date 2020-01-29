ThisBuild / resolvers := Seq(
  ("n4-public" at "http://nexus-aws.corp.numberfour.eu/nexus/content/repositories/public/")
    .withAllowInsecureProtocol(true),
  (Resolver
    .url("n4-public-ivy", new java.net.URL("http://nexus-aws.corp.numberfour.eu/nexus/content/repositories/public/"))(
      Patterns(
        "[organization]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]")))
    .withAllowInsecureProtocol(true)
)
