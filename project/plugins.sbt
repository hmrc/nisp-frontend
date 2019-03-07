resolvers += Resolver.url("HMRC Sbt Plugin Releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)
resolvers += "HMRC Releases" at "https://dl.bintray.com/hmrc/releases"
resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "1.13.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "1.15.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "1.3.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-artifactory" % "0.17.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.19")
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.5.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.6.0")
addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.8")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.1")