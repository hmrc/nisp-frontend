import play.sbt.PlayImport.PlayKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

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

val suppressedImports = Seq("-P:silencer:lineContentFilters=import _root_.play.twirl.api.TwirlFeatureImports._",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.TwirlHelperImports._",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Html",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.JavaScript",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Txt",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Xml",
  "-P:silencer:lineContentFilters=import models._",
  "-P:silencer:lineContentFilters=import controllers._",
  "-P:silencer:lineContentFilters=import play.api.i18n._",
  "-P:silencer:lineContentFilters=import views.html._",
  "-P:silencer:lineContentFilters=import play.api.templates.PlayMagic._",
  "-P:silencer:lineContentFilters=import play.api.mvc._",
  "-P:silencer:lineContentFilters=import play.api.data._",
  "-P:silencer:lineContentFilters=import uk.gov.hmrc.govukfrontend.views.html.components._",
  "-P:silencer:lineContentFilters=import uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "-P:silencer:lineContentFilters=import uk.gov.hmrc.hmrcfrontend.views.html.helpers._")

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory, SbtWeb): _*
  )
  .settings(
    publishingSettings,
    playSettings,
    scoverageSettings,
    PlayKeys.playDefaultPort := 9234,
    scalaVersion := "2.12.12",
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    update / evictionWarningOptions  := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    majorVersion := 10,
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-P:silencer:pathFilters=routes",
      "-feature"
    ),
    scalacOptions ++= suppressedImports
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / parallelExecution := false,
    scalacOptions += "-Ypartial-unification"
  )
