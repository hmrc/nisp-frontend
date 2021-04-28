resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "2.15.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "1.19.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "1.6.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-artifactory" % "0.19.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.23")
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.5.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.12")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")