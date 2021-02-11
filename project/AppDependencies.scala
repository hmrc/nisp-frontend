import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap" % "12.9.0",
    "uk.gov.hmrc" %% "http-caching-client" % "8.5.0-play-25",
    "uk.gov.hmrc" %% "play-language" % "4.2.0-play-25",
    "uk.gov.hmrc" %% "tax-year" % "0.6.0",
    "uk.gov.hmrc" %% "csp-client" % "4.1.0-play-25",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.2.0-play-25",
    "uk.gov.hmrc" %% "local-template-renderer"  % "2.8.0-play-25",
    "uk.gov.hmrc" %% "play-breadcrumb"  % "1.0.0",
    "uk.gov.hmrc" %% "auth-client"  %  "2.31.0-play-25",
    "uk.gov.hmrc" %% "play-ui" % "8.15.0-play-25"
  )

  val test = Seq(
      "org.pegdown" % "pegdown" % "1.6.0",
      "org.jsoup" % "jsoup" % "1.10.2",
      "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1",
      "uk.gov.hmrc" %% "hmrctest" % "2.4.0",
      "org.mockito" % "mockito-core" % "3.1.0"
  ).map(_ % "test")

  
  val all: Seq[ModuleID] = compile ++ test
}

