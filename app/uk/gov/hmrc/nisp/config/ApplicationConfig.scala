/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.config
import com.google.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class ApplicationConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {
  import servicesConfig._

  private def loadConfig(key: String) =
    configuration.getOptional[String](key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactFrontendService = baseUrl("contact-frontend")
  private val contactHost            = configuration.getOptional[String]("contact-frontend.host").getOrElse("")

  val appName: String                = getString("appName")
  val assetsPrefix: String           = loadConfig("assets.url") + loadConfig("assets.version") + "/"
  val betaFeedbackUrl                = s"${Constants.baseUrl}/feedback"
  val betaFeedbackUnauthenticatedUrl = betaFeedbackUrl
  val ssoUrl: Option[String]         = configuration.getOptional[String]("portal.ssoUrl")

  val frontendTemplatePath: String    = getConfString("frontend-template-provider.path", "/template/mustache")
  val frontEndTemplateProviderBaseUrl = baseUrl("frontend-template-provider")

  val contactFormServiceIdentifier  = "NISP"
  val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  val reportAProblemPartialUrl      = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  val reportAProblemNonJSUrl        = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  val showGovUkDonePage: Boolean    = configuration.getOptional[Boolean]("govuk-done-page.enabled").getOrElse(true)
  val govUkFinishedPageUrl: String  = loadConfig("govuk-done-page.url")
  val identityVerification: Boolean = getConfBool("features.identityVerification", false)

  val verifySignIn: String          = configuration.getOptional[String]("verify-sign-in.url").getOrElse("")
  val verifySignInContinue: Boolean =
    configuration.getOptional[Boolean]("verify-sign-in.submit-continue-url").getOrElse(false)
  val postSignInRedirectUrl         = configuration.getOptional[String]("login-callback.url").getOrElse("")
  val notAuthorisedRedirectUrl      = configuration.getOptional[String]("not-authorised-callback.url").getOrElse("")
  val ivUpliftUrl: String           = configuration.getOptional[String]("identity-verification-uplift.host").getOrElse("")
  val ggSignInUrl: String           = configuration.getOptional[String]("government-gateway-sign-in.host").getOrElse("")

  val showUrBanner: Boolean              = configuration.getOptional[Boolean]("urBannerToggle").getOrElse(false)
  val GaEventAction: String              = "home page UR"
  val isleManLink                        = configuration.get[String]("isle-man-link.url")
  val citizenAdviceLinkEn                = configuration.get[String]("citizens-advice-link-en.url")
  val citizenAdviceLinkCy                = configuration.get[String]("citizens-advice-link-cy.url")
  val moneyAdviceLinkEn                  = configuration.get[String]("money-advice-link-en.url")
  val moneyAdviceLinkCy                  = configuration.get[String]("money-advice-link-cy.url")
  val pensionWiseLink                    = configuration.get[String]("pension-wise-link.url")
  val frontendHost                       = loadConfig("nisp-frontend.host")
  val accessibilityStatementHost: String = loadConfig("accessibility-statement.url") + "/accessibility-statement"
  val urRecruitmentLinkURL: String       = configuration.get[String]("ur-research.url")

  private val pertaxFrontendService: String = baseUrl("pertax-frontend")
  val pertaxFrontendUrl: String             = configuration.getOptional[String]("breadcrumb-service.url").getOrElse("")
  val breadcrumbPartialUrl: String          = s"$pertaxFrontendService/personal-account/integration/main-content-header"
  val showFullNI: Boolean                   = getConfBool("features.fullNIrecord", false)
  val futureProofPersonalMax: Boolean       = getConfBool("features.future-proof.personalMax", false)
  val isWelshEnabled: Boolean               = getConfBool("features.welsh-translation", false)
  val feedbackFrontendUrl: String           = loadConfig("feedback-frontend.url")
  val futurePensionUrl: String              = configuration.getOptional[String]("future-pension-link.url").getOrElse("")
  val urBannerUrl: String                   = configuration.getOptional[String]("urBanner.link").getOrElse("")

  val citizenDetailsServiceUrl: String       = baseUrl("citizen-details")
  val identityVerificationServiceUrl: String = baseUrl("identity-verification")
  val nationalInsuranceServiceUrl: String    = baseUrl("national-insurance")
  val statePensionServiceUrl: String         = baseUrl("state-pension")
  val authServiceUrl                         = baseUrl("auth")

  val sessionCacheURL: String    = baseUrl("cachable.session-cache")
  val sessionCacheDomain: String = getConfString(
    "cachable.session-cache.domain",
    throw new Exception("Could not find config 'cachable.session-cache.domain'")
  )

  def accessibilityStatementUrl(relativeReferrerPath: String): String =
    accessibilityStatementHost + "/check-your-state-pension?referrerUrl=" + SafeRedirectUrl(
      frontendHost + relativeReferrerPath
    ).encodedUrl

}
