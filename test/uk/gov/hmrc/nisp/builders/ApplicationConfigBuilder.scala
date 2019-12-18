/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.builders

import uk.gov.hmrc.nisp.config.ApplicationConfig

object ApplicationConfigBuilder {
  def apply(assetsPrefix: String = "", betaFeedbackUrl: String = "", betaFeedbackUnauthenticatedUrl: String = "",
            analyticsToken: Option[String] = None, analyticsHost: String = "", ssoUrl: Option[String] = None,
            contactFormServiceIdentifier: String = "", contactFrontendPartialBaseUrl: String = "",
            reportAProblemPartialUrl: String = "", reportAProblemNonJSUrl: String = "", showGovUkDonePage: Boolean = false,
            govUkFinishedPageUrl: String = "", identityVerification: Boolean = false, postSignInRedirectUrl: String = "",
            notAuthorisedRedirectUrl: String = "", verifySignIn: String = "", verifySignInContinue: Boolean = false,
            ivUpliftUrl: String = "ivuplift", ggSignInUrl: String = "ggsignin", twoFactorUrl: String = "twofactor",
            pertaxFrontendUrl: String = "", breadcrumbPartialUrl: String = "", showFullNI: Boolean = false,
            futureProofPersonalMax: Boolean = false,
            isWelshEnabled: Boolean = true,
            frontendTemplatePath: String = "",
            feedbackFrontendUrl: String = "/foo"
           ): ApplicationConfig = new ApplicationConfig {
    override val assetsPrefix: String = assetsPrefix
    override val betaFeedbackUrl: String = betaFeedbackUrl
    override val betaFeedbackUnauthenticatedUrl: String = betaFeedbackUnauthenticatedUrl
    override val analyticsToken: Option[String] = analyticsToken
    override val analyticsHost: String = analyticsHost
    override val ssoUrl: Option[String] = ssoUrl
    override val contactFormServiceIdentifier: String = contactFormServiceIdentifier
    override val contactFrontendPartialBaseUrl: String = contactFormServiceIdentifier
    override val reportAProblemPartialUrl: String = reportAProblemPartialUrl
    override val reportAProblemNonJSUrl: String = reportAProblemNonJSUrl
    override val showGovUkDonePage: Boolean = showGovUkDonePage
    override val govUkFinishedPageUrl: String = govUkFinishedPageUrl
    override val identityVerification: Boolean = identityVerification
    override val postSignInRedirectUrl: String = postSignInRedirectUrl
    override val notAuthorisedRedirectUrl: String = notAuthorisedRedirectUrl
    override val verifySignIn: String = verifySignIn
    override val verifySignInContinue: Boolean = verifySignInContinue
    override val ivUpliftUrl: String = ivUpliftUrl
    override val ggSignInUrl: String = ggSignInUrl
    override val pertaxFrontendUrl: String = pertaxFrontendUrl
    override val breadcrumbPartialUrl: String = breadcrumbPartialUrl
    override lazy val showFullNI: Boolean = showFullNI
    override val futureProofPersonalMax: Boolean = futureProofPersonalMax
    override val isWelshEnabled: Boolean = isWelshEnabled
    override val frontendTemplatePath: String = frontendTemplatePath
    override val feedbackFrontendUrl: String = "/foo"
  }
}
