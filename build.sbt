import play.sbt.PlayImport.PlayKeys
import scoverage.ScoverageKeys
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

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory, SbtWeb): _*)
  .settings(publishingSettings,
    playSettings,
    scoverageSettings,
    PlayKeys.playDefaultPort := 9234,
    scalaVersion := "2.11.11",
    libraryDependencies ++= AppDependencies.all,
    //TODO will this be required after 2.6
    dependencyOverrides += "uk.gov.hmrc" %% "play-config" % "7.3.0",
    retrieveManaged := true,
    resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo,
      "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
    ),
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesGenerator := InjectedRoutesGenerator,
    majorVersion := 9
  )
