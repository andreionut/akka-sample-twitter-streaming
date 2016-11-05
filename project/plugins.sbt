logLevel := Level.Warn

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
  Resolver.ivyStylePatterns)

resolvers +=  "Kamon Repository Snapshots"  at "http://snapshots.kamon.io"

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1")

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "1.1.36")
//addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % "0.10.6")
addSbtPlugin("io.kamon" % "aspectj-runner" % "0.1.4")
