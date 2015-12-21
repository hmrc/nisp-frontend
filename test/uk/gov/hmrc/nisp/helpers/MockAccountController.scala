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

package uk.gov.hmrc.nisp.helpers

import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.AccountController
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.services.{MetricsService, NpsAvailabilityChecker}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

object MockAccountController extends MockAccountController {
  override val npsAvailabilityChecker = MockNpsAvailabilityChecker
  override val citizenDetailsService = MockCitizenDetailsService
}

trait MockAccountController extends AccountController {
  override protected implicit def authConnector: AuthConnector = MockAuthConnector
  override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
  override val npsAvailabilityChecker: NpsAvailabilityChecker

  override def metricsService: MetricsService = MockMetricsService

  override def nispConnector: NispConnector = MockNispConnector
  override val applicationConfig: ApplicationConfig = new ApplicationConfig {
    override val assetsPrefix: String = ""
    override val reportAProblemNonJSUrl: String = ""
    override val ssoUrl: Option[String] = None
    override val betaFeedbackUnauthenticatedUrl: String = ""
    override val contactFrontendPartialBaseUrl: String = ""
    override val excludeCopeTab: Boolean = false
    override val analyticsHost: String = ""
    override val analyticsToken: Option[String] = None
    override val betaFeedbackUrl: String = ""
    override val reportAProblemPartialUrl: String = ""
  }
}
