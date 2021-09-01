import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "play-partials" % "8.2.0-play-28",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.12.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.69.0-play-28",
    "uk.gov.hmrc" %% "play-ui" % "9.6.0-play-28",
    "uk.gov.hmrc" %% "domain" % "6.2.0-play-28",
    "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-28",
    "uk.gov.hmrc" %% "play-language" % "5.1.0-play-28",
    "uk.gov.hmrc" %% "tax-year" % "1.3.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.9.0-play-28",
    "uk.gov.hmrc" %% "local-template-renderer"  % "2.15.0-play-28",
    "com.typesafe.play" %% "play-json-joda" % "2.6.10",
    "com.jsuereth" %% "scala-arm" % "2.0"
  )

  val test = Seq(
      "org.pegdown" % "pegdown" % "1.6.0",
      "org.jsoup" % "jsoup" % "1.10.2",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0",
      "org.mockito" % "mockito-core" % "3.1.0",
      "com.github.tomakehurst" % "wiremock-jre8" % "2.27.0",
      "com.vladsch.flexmark" % "flexmark-all" % "0.35.10"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test
}

