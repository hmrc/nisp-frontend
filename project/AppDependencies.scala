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

import sbt.*

object AppDependencies {

  private val playVersion = "play-30"
  private val hmrcScaWrapperVersion = "1.10.0"
  private val hmrcMongoToggleVersion = "1.6.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"sca-wrapper-$playVersion"                   % hmrcScaWrapperVersion,
    "uk.gov.hmrc"       %% s"mongo-feature-toggles-client-$playVersion"  % hmrcMongoToggleVersion,
    "uk.gov.hmrc"       %% s"play-conditional-form-mapping-$playVersion" % "3.1.0",
    "uk.gov.hmrc"       %% "tax-year"                                    % "5.0.0",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% s"sca-wrapper-test-$playVersion"                   % hmrcScaWrapperVersion,
    "uk.gov.hmrc"             %% s"mongo-feature-toggles-client-test-$playVersion"  % hmrcMongoToggleVersion,
    "org.pegdown"              % "pegdown"                                          % "1.6.0",
    "org.jsoup"                % "jsoup"                                            % "1.18.1",
    "org.mockito"              % "mockito-core"                                     % "5.11.0",
    "com.github.tomakehurst"   % "wiremock"                                         % "3.0.1",
  ).map(_ % Test)

  val all: Seq[ModuleID] = compile ++ test
}
