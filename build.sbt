/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.*

val appName = "nisp-frontend"

ThisBuild / scalaVersion := "2.13.14"
ThisBuild / majorVersion := 10
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  //"-Werror", //FIXME uncomment after a full migration from V1 HttpClientModule to HttpClientV2Module
  "-Wconf:cat=unused-imports&site=.*views\\.html.*:s",
  "-Wconf:cat=unused-imports&site=<empty>:s",
  "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
  "-Wconf:cat=unused&src=.*Routes\\.scala:s",
  "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s",
  "-Wconf:cat=unused&src=.*JavaScriptReverseRoutes\\.scala:s"
)
ThisBuild / dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.3" //FIXME try removing this override after a library update

addCommandAlias("runLocal", "run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes")
addCommandAlias("runAllTests", ";test;it/test;")
addCommandAlias("runAllChecks", ";clean;scalastyle;coverageOn;runAllTests;coverageOff;coverageAggregate;")

lazy val playSettings: Seq[Setting[?]] = Seq(
  pipelineStages := Seq(digest)
)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
  "play.twirl.api.HtmlFormat"
)

lazy val scoverageSettings: Seq[Def.Setting[?]] = {
  val excludedPackages = Seq[String](
    "<empty>;Reverse.*",
    "app.*",
    "prod.*",
    "uk.gov.hmrc.nisp.auth.*",
    "uk.gov.hmrc.nisp.views.*",
    "uk.gov.hmrc.nisp.config.*",
    "uk.gov.hmrc.BuildInfo",
    "testOnlyDoNotUseInAppConf.*",
    "admin.*",
  )
  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    Seq(PlayScala, SbtDistributablesPlugin, SbtWeb) *
  )
  .settings(
    playSettings,
    scoverageSettings,
    PlayKeys.playDefaultPort := 9234,
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    update / evictionWarningOptions  := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )

val it: Project = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
