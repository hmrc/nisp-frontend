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

import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.ExclusionController
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.MockTemplateRenderer
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

object MockExclusionController extends ExclusionController {
  override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService

  override protected def authConnector: AuthConnector = MockAuthConnector
  override val applicationConfig: ApplicationConfig = new ApplicationConfig {
    override val ggSignInUrl: String = ""
    override val verifySignIn: String = ""
    override val verifySignInContinue: Boolean = false
    override val assetsPrefix: String = ""
    override val reportAProblemNonJSUrl: String = ""
    override val ssoUrl: Option[String] = None
    override val identityVerification: Boolean = true
    override val betaFeedbackUnauthenticatedUrl: String = ""
    override val notAuthorisedRedirectUrl: String = ""
    override val contactFrontendPartialBaseUrl: String = ""
    override val govUkFinishedPageUrl: String = ""
    override val showGovUkDonePage: Boolean = true
    override val analyticsHost: String = ""
    override val betaFeedbackUrl: String = ""
    override val analyticsToken: Option[String] = None
    override val reportAProblemPartialUrl: String = ""
    override val contactFormServiceIdentifier: String = ""
    override val postSignInRedirectUrl: String = ""
    override val ivUpliftUrl: String = ""
    override val pertaxFrontendUrl: String = ""
    override val breadcrumbPartialUrl: String = ""
    override val showFullNI: Boolean = false
    override val futureProofPersonalMax: Boolean = false
    override val isWelshEnabled = true
  }
  override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  override val statePensionService: StatePensionService = MockStatePensionServiceViaNisp
  override val nationalInsuranceService: NationalInsuranceService = MockNationalInsuranceServiceViaNisp
  override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
}

object MockMWRREExclusionController extends ExclusionController {
  override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService

  override protected def authConnector: AuthConnector = MockAuthConnector
  override val applicationConfig: ApplicationConfig = new ApplicationConfig {
    override val ggSignInUrl: String = ""
    override val verifySignIn: String = ""
    override val verifySignInContinue: Boolean = false
    override val assetsPrefix: String = ""
    override val reportAProblemNonJSUrl: String = ""
    override val ssoUrl: Option[String] = None
    override val identityVerification: Boolean = true
    override val betaFeedbackUnauthenticatedUrl: String = ""
    override val notAuthorisedRedirectUrl: String = ""
    override val contactFrontendPartialBaseUrl: String = ""
    override val govUkFinishedPageUrl: String = ""
    override val showGovUkDonePage: Boolean = true
    override val analyticsHost: String = ""
    override val betaFeedbackUrl: String = ""
    override val analyticsToken: Option[String] = None
    override val reportAProblemPartialUrl: String = ""
    override val contactFormServiceIdentifier: String = ""
    override val postSignInRedirectUrl: String = ""
    override val ivUpliftUrl: String = ""
    override val pertaxFrontendUrl: String = ""
    override val breadcrumbPartialUrl: String = ""
    override val showFullNI: Boolean = false
    override val futureProofPersonalMax: Boolean = false
    override val isWelshEnabled = true
  }
  override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  override val statePensionService: StatePensionService = MockStatePensionServiceViaStatePension
  override val nationalInsuranceService: NationalInsuranceService = MockNationalInsuranceServiceViaNisp
  override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
}