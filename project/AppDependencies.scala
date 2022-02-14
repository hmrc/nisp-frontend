import sbt._

object AppDependencies {

  private val playVersion = "play-28"

  private val silencerDependencies: Seq[ModuleID] = Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.0" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.0" % Provided cross CrossVersion.full
  )

  val compile = Seq(
    "uk.gov.hmrc"       %% "play-partials"                 % "8.2.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % "5.15.0",
    "uk.gov.hmrc"       %% "govuk-template"                % "5.71.0-play-28",
    "uk.gov.hmrc"       %% "play-ui"                       % "9.7.0-play-28",
    "uk.gov.hmrc"       %% "domain"                        % "6.2.0-play-28",
    "uk.gov.hmrc"       %% "http-caching-client"           % "9.5.0-play-28",
    "uk.gov.hmrc"       %% "play-language"                 % "5.1.0-play-28",
    "uk.gov.hmrc"       %% "tax-year"                      % "1.6.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.9.0-play-28",
    "uk.gov.hmrc"       %% "local-template-renderer"       % "2.16.0-play-28",
    "com.typesafe.play" %% "play-json-joda"                % "2.8.1",
    "com.jsuereth"      %% "scala-arm"                     % "2.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % s"1.27.0-$playVersion",
    "uk.gov.hmrc"       %% "govuk-template"                % s"5.69.0-$playVersion",
    "uk.gov.hmrc"       %% "play-frontend-pta"             % "0.2.0"
  )

  val test               = Seq(
    "org.pegdown"             % "pegdown"            % "1.6.0",
    "org.jsoup"               % "jsoup"              % "1.10.2",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0",
    "org.mockito"             % "mockito-core"       % "3.1.0",
    "com.github.tomakehurst"  % "wiremock-jre8"      % "2.27.0",
    "com.vladsch.flexmark"    % "flexmark-all"       % "0.35.10"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test ++ silencerDependencies
}
