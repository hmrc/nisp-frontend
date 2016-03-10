/*
 * Copyright 2015 HM Revenue & Customs
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

import sbt._

object FrontendBuild extends Build with MicroService {
  import com.typesafe.sbt.web.SbtWeb.autoImport._
  import sbt.Keys._
  import play.PlayImport.PlayKeys._

  val appName = "nisp-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()

  override lazy val playSettings : Seq[Setting[_]] = Seq(
    routesImport ++= Seq("uk.gov.hmrc.domain._"),
    // Turn off play's internal less compiler
    lessEntryPoints := Nil,
    // Turn off play's internal javascript compiler
    javascriptEntryPoints := Nil,
    // Add the views to the dist
    unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
    // Dont include the source assets in the dist package (public folder)
    excludeFilter in Assets := "js*" || "sass*"
  ) ++ JavaScriptBuild.javaScriptUiSettings
}

private object AppDependencies {
  import play.core.PlayVersion

  private val playHealthVersion = "1.1.0"
  private val govukTemplateVersion = "4.0.0"
  private val httpCachingClientVersion = "5.3.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap" % "5.1.1",
    "uk.gov.hmrc" %% "play-partials" % "4.2.0",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "4.5.0",
    "uk.gov.hmrc" %% "play-config" % "2.0.1",
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.1",
    "uk.gov.hmrc" %% "play-health" % "1.1.0",
    "uk.gov.hmrc" %% "govuk-template" % "4.0.0",
    "uk.gov.hmrc" %% "play-ui" % "4.10.0",
    "uk.gov.hmrc" %% "url-builder" % "1.0.0",
    "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.8",
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "domain" % "3.2.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "1.4.0" % scope,
        "org.scalatest" %% "scalatest" % "2.2.2" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "org.jsoup" % "jsoup" % "1.7.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "org.scalatestplus" % "play_2.11" % "1.2.0" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}


