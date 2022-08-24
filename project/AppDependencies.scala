import sbt._

object AppDependencies {

  private val playVersion = "play-28"

  private val silencerDependencies: Seq[ModuleID] = Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.0" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.0" % Provided cross CrossVersion.full
  )

  val compile = Seq(
    "uk.gov.hmrc"       %% "play-partials"                 % s"8.3.0-$playVersion",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % "7.0.0",
    "uk.gov.hmrc"       %% "govuk-template"                % s"5.77.0-$playVersion",
    "uk.gov.hmrc"       %% "play-ui"                       % s"9.10.0-$playVersion",
    "uk.gov.hmrc"       %% "domain"                        % s"8.1.0-$playVersion",
    "uk.gov.hmrc"       %% "http-caching-client"           % s"9.6.0-$playVersion",
    "uk.gov.hmrc"       %% "play-language"                 % s"5.3.0-$playVersion",
    "uk.gov.hmrc"       %% "tax-year"                      % "3.0.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % s"1.11.0-$playVersion",
    "uk.gov.hmrc"       %% "local-template-renderer"       % s"2.17.0-$playVersion",
    "com.jsuereth"      %% "scala-arm"                     % "2.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % s"1.27.0-$playVersion",
    "uk.gov.hmrc"       %% "govuk-template"                % s"5.77.0-$playVersion",
    "uk.gov.hmrc"       %% "play-frontend-pta"             % "0.3.0"
  )

  val test               = Seq(
    "org.pegdown"             % "pegdown"            % "1.6.0",
    "org.jsoup"               % "jsoup"              % "1.14.3",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0",
    "org.mockito"             % "mockito-core"       % "4.6.1",
    "com.github.tomakehurst"  % "wiremock-jre8"      % "2.27.2",
    "com.vladsch.flexmark"    % "flexmark-all"       % "0.35.10"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test ++ silencerDependencies
}
