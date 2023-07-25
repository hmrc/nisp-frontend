/*
 * Copyright 2023 HM Revenue & Customs
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

import play.sbt.PlayImport.ehcache
import sbt._

object AppDependencies {

  val bootstrapVersion = "7.12.0"
  private val playVersion = "play-28"
  private val hmrcMongoVersion = "0.74.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVersion"   % bootstrapVersion,
    "uk.gov.hmrc"       %% "domain"                             % s"8.1.0-$playVersion",
    "uk.gov.hmrc"       %% "http-caching-client"                % s"10.0.0-$playVersion",
    "uk.gov.hmrc"       %% "tax-year"                           % "3.0.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"      % s"1.12.0-$playVersion",
    "uk.gov.hmrc"       %% "play-partials"                      % s"8.3.0-$playVersion",
    "uk.gov.hmrc"       %% "play-frontend-pta"                  % "0.4.0",
    "uk.gov.hmrc"       %% s"internal-auth-client-$playVersion" % "1.2.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"           % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "mongo-feature-toggles-client"       % "0.2.0",
    "uk.gov.hmrc"       %% "sca-wrapper"                        % "1.0.40",
    ehcache
  )

  val test: Seq[ModuleID] = Seq(
    "org.pegdown"             %   "pegdown"                 % "1.6.0",
    "org.jsoup"               %   "jsoup"                   % "1.15.4",
    "uk.gov.hmrc"             %%  "bootstrap-test-play-28"  % bootstrapVersion,
    "org.mockito"             %   "mockito-core"            % "4.6.1",
    "com.github.tomakehurst"  %   "wiremock-jre8"           % "2.27.2",
    "com.vladsch.flexmark"    %   "flexmark-all"            % "0.35.10",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test
}
