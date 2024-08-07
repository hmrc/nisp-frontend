resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns
)

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"

addSbtPlugin("uk.gov.hmrc"          % "sbt-auto-build"         % "3.22.0")
addSbtPlugin("uk.gov.hmrc"          % "sbt-distributables"     % "2.5.0")
addSbtPlugin("org.playframework"    % "sbt-plugin"             % "3.0.3")
addSbtPlugin("org.scoverage"       %% "sbt-scoverage"          % "2.1.0")
addSbtPlugin("org.scalastyle"      %% "scalastyle-sbt-plugin"  % "1.0.0" exclude("org.scala-lang.modules", "scala-xml_2.12"))
addSbtPlugin("io.github.irundaia"   % "sbt-sassify"            % "1.5.2")
addSbtPlugin("com.typesafe.sbt"     % "sbt-digest"             % "1.1.4")
addSbtPlugin("org.scalameta"        % "sbt-scalafmt"           % "2.5.2")
