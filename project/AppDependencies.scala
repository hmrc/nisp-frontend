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

import play.sbt.PlayImport.ehcache
import sbt.*

object AppDependencies {

  val bootstrapVersion = "8.5.0"
  private val playVersion = "play-30"
  private val hmrcMongoVersion = "1.9.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"http-caching-client-$playVersion"           % "11.2.0",
    "uk.gov.hmrc"       %% "tax-year"                                    % "4.0.0",
    "uk.gov.hmrc"       %% s"play-conditional-form-mapping-$playVersion" % "2.0.0",
    "uk.gov.hmrc"       %% s"mongo-feature-toggles-client-$playVersion"  % "1.3.0",
    "uk.gov.hmrc"       %% s"sca-wrapper-$playVersion"                   % "1.6.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.pegdown"             %   "pegdown"                       % "1.6.0",
    "org.jsoup"               %   "jsoup"                         % "1.15.4",
    "uk.gov.hmrc"             %%  s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "org.mockito"             %   "mockito-core"                  % "5.8.0",
    "com.github.tomakehurst"  %   "wiremock"                      % "2.27.2",
    "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-test-$playVersion"  % hmrcMongoVersion
  ).map(_ % Test)

  val all: Seq[ModuleID] = compile ++ test
}
