import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-26" % "3.3.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.61.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.21.0-play-26",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-26",
    "uk.gov.hmrc" %% "time" % "3.2.0",
    "uk.gov.hmrc" %% "http-caching-client" % "9.2.0-play-26",
    "uk.gov.hmrc" %% "play-language" % "4.7.0-play-26",
    "uk.gov.hmrc" %% "tax-year" % "0.6.0",
    "uk.gov.hmrc" %% "csp-client" % "4.2.0-play-26",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.5.0-play-26",
    "uk.gov.hmrc" %% "local-template-renderer"  % "2.10.0-play-26",
    "uk.gov.hmrc" %% "play-breadcrumb"  % "1.0.0",
    "uk.gov.hmrc" %% "auth-client"  %  "3.2.0-play-26",
    "com.typesafe.play" %% "play-json-joda" % "2.6.10"
  )

  val test = Seq(
      "org.pegdown" % "pegdown" % "1.6.0",
      "org.jsoup" % "jsoup" % "1.10.2",
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0",
      "uk.gov.hmrc" %% "hmrctest" % "3.10.0-play-26",
      "org.mockito" % "mockito-core" % "3.1.0"
  ).map(_ % "test")

  
  val all: Seq[ModuleID] = compile ++ test
}

