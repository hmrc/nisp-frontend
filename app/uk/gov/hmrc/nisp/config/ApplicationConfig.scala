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
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.config.ServicesConfig


//TODO temp object until views are injected
object ApplicationConfig extends ApplicationConfig(Play.current.configuration)

class ApplicationConfig @Inject()(configuration: Configuration) extends ServicesConfig {


  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactFrontendService = baseUrl("contact-frontend")
  private val contactHost = configuration.getString("contact-frontend.host").getOrElse("")

  //TODO use @Named for 2.6
  val appName: String = configuration.getString("appName").getOrElse("APP NAME NOT SET")
  val assetsPrefix: String = loadConfig("assets.url") + loadConfig("assets.version") + "/"
  val betaFeedbackUrl = s"${Constants.baseUrl}/feedback"
  val betaFeedbackUnauthenticatedUrl = betaFeedbackUrl
  val analyticsToken: Option[String] = configuration.getString(s"google-analytics.token")
  val analyticsHost: String = configuration.getString(s"google-analytics.host").getOrElse("auto")
  val ssoUrl: Option[String] = configuration.getString(s"portal.ssoUrl")

  val frontendTemplatePath: String = configuration.getString("microservice.services.frontend-template-provider.path").getOrElse("/template/mustache")
  val frontEndTemplateProviderBaseUrl = baseUrl("frontend-template-provider")

  val googleTagManagerId = loadConfig("google-tag-manager.id")
  val isGtmEnabled = configuration.getBoolean("google-tag-manager.enabled").getOrElse(false)
  val contactFormServiceIdentifier = "NISP"
  val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  val showGovUkDonePage: Boolean = configuration.getBoolean("govuk-done-page.enabled").getOrElse(true)
  val govUkFinishedPageUrl: String = loadConfig("govuk-done-page.url")
  val identityVerification: Boolean = configuration.getBoolean("microservice.services.features.identityVerification").getOrElse(false)

  val verifySignIn: String = configuration.getString("verify-sign-in.url").getOrElse("")
  val verifySignInContinue: Boolean = configuration.getBoolean("verify-sign-in.submit-continue-url").getOrElse(false)
  val postSignInRedirectUrl = configuration.getString("login-callback.url").getOrElse("")
  val notAuthorisedRedirectUrl = configuration.getString("not-authorised-callback.url").getOrElse("")
  val ivUpliftUrl: String = configuration.getString("identity-verification-uplift.host").getOrElse("")
  val ggSignInUrl: String = configuration.getString("government-gateway-sign-in.host").getOrElse("")

  val showUrBanner:Boolean = configuration.getBoolean("urBannerToggle").getOrElse(false)
  val GaEventAction: String = "home page UR"
  val isleManLink = runModeConfiguration.getString("isle-man-link.url")
  val citizenAdviceLinkEn = runModeConfiguration.getString("citizens-advice-link-en.url")
  val citizenAdviceLinkCy = runModeConfiguration.getString("citizens-advice-link-cy.url")
  val moneyAdviceLinkEn = runModeConfiguration.getString("money-advice-link-en.url")
  val moneyAdviceLinkCy = runModeConfiguration.getString("money-advice-link-cy.url")
  val pensionWiseLink = runModeConfiguration.getString("pension-wise-link.url")
  val frontendHost = loadConfig("nisp-frontend.host")
  val accessibilityStatementHost: String = loadConfig("accessibility-statement.url") + "/accessibility-statement"

  private val pertaxFrontendService: String = baseUrl("pertax-frontend")
  val pertaxFrontendUrl: String = configuration.getString("breadcrumb-service.url").getOrElse("")
  val breadcrumbPartialUrl: String = s"$pertaxFrontendService/personal-account/integration/main-content-header"
  val showFullNI: Boolean = configuration.getBoolean("microservice.services.features.fullNIrecord").getOrElse(false)
  val futureProofPersonalMax: Boolean = configuration.getBoolean("microservice.services.features.future-proof.personalMax").getOrElse(false)
  val isWelshEnabled: Boolean = configuration.getBoolean("microservice.services.features.welsh-translation").getOrElse(false)
  val feedbackFrontendUrl: String = loadConfig("feedback-frontend.url")

  val citizenDetailsServiceUrl: String = baseUrl("citizen-details")
  val identityVerificationServiceUrl: String = baseUrl("identity-verification")
  val nationalInsuranceServiceUrl: String = baseUrl("national-insurance")
  val statePensionServiceUrl: String = baseUrl("state-pension")
  val authServiceUrl =  baseUrl("auth")

  val sessionCacheURL: String = baseUrl("cachable.session-cache")
  val sessionCacheDomain: String = getConfString("cachable.session-cache.domain", throw new Exception("Could not find config 'cachable.session-cache.domain'"))

  def accessibilityStatementUrl(relativeReferrerPath: String): String =
    accessibilityStatementHost + "/check-your-state-pension?referrerUrl=" + SafeRedirectUrl(frontendHost + relativeReferrerPath).encodedUrl

  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
