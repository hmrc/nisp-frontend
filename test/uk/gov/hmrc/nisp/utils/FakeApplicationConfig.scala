/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.utils

import uk.gov.hmrc.nisp.config.ApplicationConfig

trait FakeApplicationConfig extends ApplicationConfig  {
  override val ggSignInUrl: String = ""
  override val verifySignIn: String = ""
  override val verifySignInContinue: Boolean = false
  override val assetsPrefix: String = ""
  override val reportAProblemNonJSUrl: String = ""
  override val ssoUrl: Option[String] = None
  override val identityVerification: Boolean = false
  override val betaFeedbackUnauthenticatedUrl: String = ""
  override val notAuthorisedRedirectUrl: String = ""
  override val contactFrontendPartialBaseUrl: String = ""
  override val govUkFinishedPageUrl: String = ""
  override val showGovUkDonePage: Boolean = false
  override val analyticsHost: String = ""
  override val betaFeedbackUrl: String = ""
  override val analyticsToken: Option[String] = None
  override val reportAProblemPartialUrl: String = ""
  override val contactFormServiceIdentifier: String = "NISP"
  override val postSignInRedirectUrl: String = ""
  override val ivUpliftUrl: String = ""
  override val pertaxFrontendUrl: String = ""
  override val breadcrumbPartialUrl: String = ""
  override lazy val showFullNI: Boolean = false
  override val futureProofPersonalMax: Boolean = false
  override val isWelshEnabled = false
  override val frontendTemplatePath: String = "microservice.services.frontend-template-provider.path"
  override val feedbackFrontendUrl: String = "/foo"
}