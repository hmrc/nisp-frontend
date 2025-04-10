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

val appName = "nisp-frontend"

ThisBuild / scalaVersion := "3.6.4"
ThisBuild / majorVersion := 10
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-Xfatal-warnings",
  "-Wconf:src=routes/.*:silent", // Suppress warnings from routes files
  "-Wconf:src=twirl/.*:silent",  // Suppress warnings from twirl files
  "-Wconf:src=target/.*:silent", // Suppress warnings from target files
  "-Wconf:msg=Flag.*repeatedly:silent", // Suppress repeated flag warnings
)

val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin, SbtWeb)
  .settings(
    PlayKeys.playDefaultPort := 9234,
    CodeCoverageSettings(),
    pipelineStages := Seq(digest),
    libraryDependencies ++= AppDependencies(),
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "play.twirl.api.HtmlFormat"
    )
  )

val it: Project =
  project.in(file("it"))
    .enablePlugins(PlayScala)
    .dependsOn(microservice % "test->test")

addCommandAlias("runLocal", "run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes")
addCommandAlias("runLocalWithColours", "run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes -Dlogger.resource=logback-colours.xml")
addCommandAlias("runAllTests", ";test;it/test;")
addCommandAlias("runAllTestsWithColours", ";set Test/javaOptions += \"-Dlogger.resource=logback-colours.xml\";runAllTests;")
addCommandAlias("runAllChecks", ";clean;coverageOn;runAllTests;coverageOff;coverageAggregate;")
