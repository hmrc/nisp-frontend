import sbt._

object AppDependencies {

  val bootstrapVersion = "7.12.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-28"    % bootstrapVersion,
    "uk.gov.hmrc"   %% "domain"                        % "8.1.0-play-28",
    "uk.gov.hmrc"   %% "http-caching-client"           % "10.0.0-play-28",
    "uk.gov.hmrc"   %% "tax-year"                      % "3.0.0",
    "uk.gov.hmrc"   %% "play-conditional-form-mapping" % "1.12.0-play-28",
    "uk.gov.hmrc"   %% "play-frontend-hmrc"            % "6.7.0-play-28",
    "uk.gov.hmrc"   %% "play-frontend-pta"             % "0.4.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.pegdown"             %   "pegdown"                 % "1.6.0",
    "org.jsoup"               %   "jsoup"                   % "1.15.4",
    "uk.gov.hmrc"             %%  "bootstrap-test-play-28"  % bootstrapVersion,
    "org.mockito"             %   "mockito-core"            % "4.6.1",
    "com.github.tomakehurst"  %   "wiremock-jre8"           % "2.27.2",
    "com.vladsch.flexmark"    %   "flexmark-all"            % "0.35.10"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test
}
