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

package uk.gov.hmrc.nisp.config

import java.net.URLEncoder
import play.api.Play._
import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.nisp.controllers.auth.NispUser
import uk.gov.hmrc.nisp.controllers.routes
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.config.ServicesConfig

trait ApplicationConfig {
  val assetsPrefix: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val analyticsToken: Option[String]
  val analyticsHost: String
  val ssoUrl: Option[String]
  val contactFormServiceIdentifier: String
  val contactFrontendPartialBaseUrl: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val showGovUkDonePage: Boolean
  val govUkFinishedPageUrl: String
  val identityVerification: Boolean
  val citizenAuthHost: String
  val postSignInRedirectUrl: String
  val notAuthorisedRedirectUrl: String
  val verifySignIn = s"$citizenAuthHost/ida/login"
  val ivUpliftUrl: String
  val twoFactorUrl: String
  val ggSignInUrl: String
  val pertaxFrontendUrl: String
  val breadcrumbPartialUrl: String
  val showFullNI: Boolean
  val futureProofPersonalMax: Boolean
  val copeTable: Boolean
  val useStatePensionAPI: Boolean
  var assetVersion: String
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  /*var assetVersion is used to cache bust js/css files in production mode. Change this value when there is change in any of css or js files*/
  var assetVersion = "1.0.0"

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactFrontendService = baseUrl("contact-frontend")
  private val contactHost = configuration.getString(s"contact-frontend.host").getOrElse("")

  override lazy val assetsPrefix: String = loadConfig(s"assets.url") + loadConfig(s"assets.version")
  override lazy val betaFeedbackUrl = s"${Constants.baseUrl}/feedback"
  override lazy val betaFeedbackUnauthenticatedUrl = betaFeedbackUrl
  override lazy val analyticsToken: Option[String] = configuration.getString(s"google-analytics.token")
  override lazy val analyticsHost: String = configuration.getString(s"google-analytics.host").getOrElse("auto")
  override lazy val ssoUrl: Option[String] = configuration.getString(s"portal.ssoUrl")

  override val contactFormServiceIdentifier = "NISP"
  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override val showGovUkDonePage: Boolean = configuration.getBoolean("govuk-done-page.enabled").getOrElse(true)
  override val govUkFinishedPageUrl: String = loadConfig("govuk-done-page.url")
  override val identityVerification: Boolean = configuration.getBoolean("microservice.services.features.identityVerification").getOrElse(false)

  override lazy val citizenAuthHost = configuration.getString("citizen-auth.host").getOrElse("")
  override lazy val postSignInRedirectUrl = configuration.getString("login-callback.url").getOrElse("")
  override lazy val notAuthorisedRedirectUrl = configuration.getString("not-authorised-callback.url").getOrElse("")
  override val ivUpliftUrl: String = configuration.getString(s"identity-verification-uplift.host").getOrElse("")
  override val ggSignInUrl: String = configuration.getString(s"government-gateway-sign-in.host").getOrElse("")
  override val twoFactorUrl: String = configuration.getString(s"two-factor.host").getOrElse("")

  private val pertaxFrontendService: String = baseUrl("pertax-frontend")
  override lazy val pertaxFrontendUrl: String = configuration.getString(s"breadcrumb-service.url").getOrElse("")
  override lazy val breadcrumbPartialUrl: String = s"$pertaxFrontendService/personal-account/integration/main-content-header"
  override lazy val showFullNI: Boolean = configuration.getBoolean("microservice.services.features.fullNIrecord").getOrElse(false)
  override lazy val futureProofPersonalMax: Boolean = configuration.getBoolean("microservice.services.features.future-proof.personalMax").getOrElse(false)
  override lazy val copeTable: Boolean = configuration.getBoolean("microservice.services.features.copeTable").getOrElse(false)
  override val useStatePensionAPI: Boolean = configuration.getBoolean("microservice.services.features.useStatePensionAPI").getOrElse(true)
}
