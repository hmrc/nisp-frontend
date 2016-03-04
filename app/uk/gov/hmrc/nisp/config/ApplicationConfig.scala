/*
 * Copyright 2016 HM Revenue & Customs
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

import java.net.{URLEncoder, URI}

import play.api.Play._
import play.api.i18n.Messages
import uk.gov.hmrc.nisp.controllers.auth.NispUser
import uk.gov.hmrc.nisp.controllers.routes
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.nisp.utils.Constants
import play.api.mvc.Request

trait ApplicationConfig {
  val assetsPrefix: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val analyticsToken: Option[String]
  val analyticsHost: String
  val ssoUrl: Option[String]
  val contactFrontendPartialBaseUrl: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val excludeCopeTab: Boolean
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
  val breadcrumbServiceUrl: String
  val perTaxFrontEndUrl: String
  val initialBreadCrumbList: List[(String, String)]
  val mainContentHeaderPartialUrl: String
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {
  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactFrontendService = baseUrl("contact-frontend")
  private val contactHost = configuration.getString(s"contact-frontend.host").getOrElse("")

  val contactFormServiceIdentifier = "NISP"

  override lazy val assetsPrefix: String = loadConfig(s"assets.url") + loadConfig(s"assets.version")
  override lazy val betaFeedbackUrl = s"${Constants.baseUrl}/feedback"
  override lazy val betaFeedbackUnauthenticatedUrl = betaFeedbackUrl
  override lazy val analyticsToken: Option[String] = configuration.getString(s"google-analytics.token")
  override lazy val analyticsHost: String = configuration.getString(s"google-analytics.host").getOrElse("auto")
  override lazy val ssoUrl: Option[String] = configuration.getString(s"portal.ssoUrl")

  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override val excludeCopeTab: Boolean = configuration.getBoolean(s"microservice.services.exclusions.copetab").getOrElse(true)
  override val showGovUkDonePage: Boolean = configuration.getBoolean("govuk-done-page.enabled").getOrElse(true)
  override val govUkFinishedPageUrl: String = loadConfig("govuk-done-page.url")
  override val identityVerification: Boolean = configuration.getBoolean("microservice.services.features.identityVerification").getOrElse(false)

  override lazy val citizenAuthHost = configuration.getString("citizen-auth.host").getOrElse("")
  override lazy val postSignInRedirectUrl = configuration.getString("login-callback.url").getOrElse("")
  override lazy val notAuthorisedRedirectUrl = configuration.getString("not-authorised-callback.url").getOrElse("")
  override val ivUpliftUrl: String = configuration.getString(s"identity-verification-uplift.host").getOrElse("")
  override val ggSignInUrl: String = configuration.getString(s"government-gateway-sign-in.host").getOrElse("")
  override val twoFactorUrl: String = configuration.getString(s"two-factor.host").getOrElse("")
  override lazy val breadcrumbServiceUrl: String = configuration.getString(s"breadcrumb-service.url").getOrElse("")

  override lazy val perTaxFrontEndUrl: String  = configuration.getString(s"pertax-frontend.url").getOrElse("")
  lazy val pertaxServiceUrl = s"$perTaxFrontEndUrl/"
  val initialBreadCrumbList = List(
                        (URLEncoder.encode(Messages("nisp.breadcrumb.account"),"UTF-8"), pertaxServiceUrl)
                        )

  lazy val mainContentHeaderPartialUrl = s"$breadcrumbServiceUrl/integration/main-content-header"

  private[config] def buildBreadCrumb(request: Request[_]): List[(String, String)] = {
    val links = Map(
      "account" -> ((URLEncoder.encode(Messages("nisp.breadcrumb.pension"), "UTF-8"), routes.AccountController.show().url)),
      "nirecord" -> ((URLEncoder.encode(Messages("nisp.breadcrumb.nirecord"), "UTF-8"), routes.NIRecordController.showGaps().url)),
      "voluntarycontribs" -> ((URLEncoder.encode(Messages("nisp.breadcrumb.nirecord"), "UTF-8"), routes.NIRecordController.showVoluntaryContributions().url)),
      "gapsandhowtocheck" -> ((URLEncoder.encode(Messages("nisp.breadcrumb.nirecord"), "UTF-8"), routes.NIRecordController.showGapsAndHowToCheckThem().url)),
      "exclusion" -> ((URLEncoder.encode(Messages("nisp.breadcrumb.account"), "UTF-8"), pertaxServiceUrl))
    )
    try {
      val items = request.path.split("/").filter(!_.isEmpty).map(links.get).toList
      initialBreadCrumbList ::: items.flatten
    } catch {
      case e: NoSuchElementException => Nil
    }
  }

  def generateHeaderUrl() (implicit request:Request[_], user: NispUser): String = {
    val userName = if(Some(user.name).isDefined) {
      user.name.get
    } else {
      "UserNotFound"
    }

    mainContentHeaderPartialUrl + "?name=" + s"${URLEncoder.encode(userName,"UTF-8")}" +"&" +
      user.previouslyLoggedInAt.map("lastLogin=" + _.getMillis + "&").getOrElse("") +
      buildBreadCrumb(request).map(listItem => s"item_text=${listItem._1}&item_url=${listItem._2}").mkString("&") +
      "&showBetaBanner=false&deskProToken='NISP'"
  }
}