import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "nisp-frontend"

lazy val microservice: Project = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    publishingSettings,
    scalaSettings,
    defaultSettings(),
    targetJvm := "jvm-1.8",
    scalaVersion := "2.11.11",
    PlayKeys.playDefaultPort := 9234,
    libraryDependencies ++= AppDependencies.all,
    parallelExecution in Test := false,
    fork in Test := false,
    retrieveManaged := true,
    routesGenerator := StaticRoutesGenerator,
    resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo,
      "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
    ),
    majorVersion := 10)