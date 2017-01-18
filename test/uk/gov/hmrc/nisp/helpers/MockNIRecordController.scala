/*
 * Copyright 2017 HM Revenue & Customs
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

import org.joda.time.LocalDate
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.NIRecordController
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.services.{MetricsService, CitizenDetailsService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever

object MockNIRecordController extends MockNIRecordController {
  override val nispConnector: NispConnector = MockNispConnector
  override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
  override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
  override val sessionCache: SessionCache = MockSessionCache
  override val showFullNI: Boolean = true
  override val currentDate = new LocalDate(2016,9,9)
  override val metricsService: MetricsService = MockMetricsService
}

trait MockNIRecordController extends NIRecordController {
  override protected def authConnector: AuthConnector = MockAuthConnector

  override val applicationConfig: ApplicationConfig = new ApplicationConfig {
    override val assetsPrefix: String = ""
    override val reportAProblemNonJSUrl: String = ""
    override val ssoUrl: Option[String] = None
    override val betaFeedbackUnauthenticatedUrl: String = ""
    override val contactFrontendPartialBaseUrl: String = ""
    override val analyticsHost: String = ""
    override val analyticsToken: Option[String] = None
    override val betaFeedbackUrl: String = ""
    override val reportAProblemPartialUrl: String = ""
    override val showGovUkDonePage: Boolean = true
    override val govUkFinishedPageUrl: String = "govukdone"
    override val citizenAuthHost: String = ""
    override val postSignInRedirectUrl: String = ""
    override val notAuthorisedRedirectUrl: String = ""
    override val identityVerification: Boolean = true
    override val ivUpliftUrl: String = "ivuplift"
    override val ggSignInUrl: String = "ggsignin"
    override val twoFactorUrl: String = "twofactor"
    override val pertaxFrontendUrl: String = ""
    override val contactFormServiceIdentifier: String = ""
    override val breadcrumbPartialUrl: String = ""
    override val showFullNI: Boolean = true
    override val futureProofPersonalMax: Boolean = false
    override val copeTable: Boolean = false
    override val useStatePensionAPI: Boolean = true
  }
  override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever

}
