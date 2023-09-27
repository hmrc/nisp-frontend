import play.sbt.PlayImport.PlayKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption

val appName = "nisp-frontend"

lazy val playSettings: Seq[Setting[_]] = Seq(
  pipelineStages := Seq(digest)
)

val excludedPackages = Seq[String](
  "<empty>;Reverse.*",
  "app.*",
  "prod.*",
  "uk.gov.hmrc.nisp.auth.*",
  "uk.gov.hmrc.nisp.views.*",
  "uk.gov.hmrc.nisp.config.*",
  "uk.gov.hmrc.BuildInfo"
)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

lazy val scoverageSettings =
  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 87.36,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    Seq(play.sbt.PlayScala, SbtDistributablesPlugin, SbtWeb): _*
  )
  .settings(
    playSettings,
    scoverageSettings,
    PlayKeys.playDefaultPort := 9234,
    scalaVersion := "2.13.8",
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    update / evictionWarningOptions  := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    majorVersion := 10,
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-feature",
      "-Werror",
      "-Wconf:cat=unused-imports&site=.*views\\.html.*:s",
      "-Wconf:cat=unused-imports&site=<empty>:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s",
      "-Wconf:cat=unused&src=.*JavaScriptReverseRoutes\\.scala:s"
    )
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / parallelExecution := false
  )
