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
import uk.gov.hmrc.nisp.utils.Breadcrumb

object MockBreadcrumb extends Breadcrumb {
  override lazy val applicationConfig: ApplicationConfig = new ApplicationConfig {
    override val ggSignInUrl: String = ""
    override val verifySignIn: String = ""
    override val verifySignInContinue: Boolean = false
    override val twoFactorUrl: String = ""
    override val assetsPrefix: String = ""
    override val reportAProblemNonJSUrl: String = ""
    override val ssoUrl: Option[String] = None
    override val identityVerification: Boolean = false
    override val betaFeedbackUnauthenticatedUrl: String = ""
    override val notAuthorisedRedirectUrl: String = ""
    override val contactFrontendPartialBaseUrl: String = ""
    override val govUkFinishedPageUrl: String = ""
    override val showGovUkDonePage: Boolean = true
    override val analyticsHost: String = ""
    override val betaFeedbackUrl: String = ""
    override val analyticsToken: Option[String] = None
    override val reportAProblemPartialUrl: String = ""
    override val postSignInRedirectUrl: String = ""
    override val ivUpliftUrl: String = ""
    override val pertaxFrontendUrl: String = "http://localhost:9232/account"
    override val contactFormServiceIdentifier: String = ""
    override val breadcrumbPartialUrl: String = "http://localhost:9232/account"
    override val showFullNI: Boolean = false
    override val futureProofPersonalMax: Boolean = false
    override val useStatePensionAPI: Boolean = true
    override val useNationalInsuranceAPI: Boolean = true
    override val isWelshEnabled = false
  }
}
