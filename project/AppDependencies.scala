import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "play-partials" % "8.1.0-play-26",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-26" % "5.3.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.61.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.21.0-play-26",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-26",
    "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-26",
    "uk.gov.hmrc" %% "play-language" % "5.1.0-play-26",
    "uk.gov.hmrc" %% "tax-year" % "1.3.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.5.0-play-26",
    "uk.gov.hmrc" %% "local-template-renderer"  % "2.10.0-play-26",
    "com.typesafe.play" %% "play-json-joda" % "2.6.10",
    "com.jsuereth" %% "scala-arm" % "2.0"
  )

  val test = Seq(
      "org.pegdown" % "pegdown" % "1.6.0",
      "org.jsoup" % "jsoup" % "1.10.2",
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0",
      "uk.gov.hmrc" %% "hmrctest" % "3.10.0-play-26",
      "org.mockito" % "mockito-core" % "3.1.0",
      "com.github.tomakehurst" % "wiremock-jre8" % "2.27.0"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test
}

