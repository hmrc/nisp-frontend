/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{RedirectUrl, UnsafePermitAll}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class ApplicationConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {
  import servicesConfig._

  private val contactHost            = servicesConfig.getString("contact-frontend.host")

  val appName: String                = getString("appName")

  val serviceUrl: String            = servicesConfig.getString("serviceUrl")
  val contactFormServiceIdentifier  = "NISP"
  val reportAProblemNonJSUrl        = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  val postSignInRedirectUrl: String     = getString("login-callback.url")
  val notAuthorisedRedirectUrl: String  = getString("not-authorised-callback.url")
  val ivUpliftUrl: String               = getString("identity-verification-uplift.host")
  val mfaUpliftUrl: String              = getString("mfa-uplift.url")
  val ggSignInUrl: String               = getString("government-gateway-sign-in.host")

  val showExcessiveTrafficMessage: Boolean  = getBoolean("excessiveTrafficToggle")
  val isleManLink: String                   = getString("isle-man-link.url")
  val citizenAdviceLinkEn: String           = getString("citizens-advice-link-en.url")
  val citizenAdviceLinkCy: String           = getString("citizens-advice-link-cy.url")
  val moneyAdviceLinkEn: String             = getString("money-advice-link-en.url")
  val moneyAdviceLinkCy: String             = getString("money-advice-link-cy.url")
  val pensionWiseLink: String               = getString("pension-wise-link.url")
  val pensionWiseLinkCy: String             = getString("pension-wise-link-cy.url")

  val futurePensionLink: String             = getString("govUkLinks.future-pension-link.url")
  val nationalInsuranceLink: String         = getString("govUkLinks.national-insurance-link.url")
  val niHowMuchYouPayLink: String           = getString("govUkLinks.ni-how-much-you-pay-link.url")
  val nationalInsuranceCreditLink: String   = getString("govUkLinks.national-insurance-credits-link.url")
  val additionalStatePensionLink: String    = getString("govUkLinks.additional-state-pension-link.url")
  val pensionDeferralLink: String           = getString("govUkLinks.pension-deferral-link.url")
  val newStatePensionLink: String           = getString("govUkLinks.new-state-pension-link.url")
  val copeLink: String                      = getString("govUkLinks.contracted-out-pension-link.url")
  val homeResponsibilitiesLink: String      = getString("govUkLinks.home-responsibilities-protection-link.url")
  val pensionCreditLink: String             = getString("govUkLinks.pension-credit-link.url")
  val abroadLink: String                    = getString("govUkLinks.living-and-working-overseas-link.url")

  private val frontendHost                  = getString("nisp-frontend.host")
  private val accessibilityStatementHost: String    = getString("accessibility-statement.url") + "/accessibility-statement"

  val showFullNI: Boolean                   = getConfBool("features.fullNIrecord", false)
  val futureProofPersonalMax: Boolean       = getConfBool("features.future-proof.personalMax", false)
  val isWelshEnabled: Boolean               = getConfBool("features.welsh-translation", false)
  val feedbackFrontendUrl: String           = getString("feedback-frontend.url")
  val niRecordPayableYears: Int             = getInt("numberOfPayableTaxYears")
  val friendlyUsers: Seq[String]            = config.get[Seq[String]]("allowedUsers.friendly")
  val allowedUsersEndOfNino: Seq[String]    = config.get[Seq[String]]("allowedUsers.endOfNino")
  val nispModellingFrontendUrl: String      = getString("nisp-modelling.url")

  val citizenDetailsServiceUrl: String       = baseUrl("citizen-details")
  val identityVerificationServiceUrl: String = baseUrl("identity-verification")
  val nationalInsuranceServiceUrl: String    = baseUrl("national-insurance")
  val statePensionServiceUrl: String         = baseUrl("state-pension")

  val pertaxAuthBaseUrl: String = baseUrl("pertax-auth")

  def accessibilityStatementUrl(relativeReferrerPath: String): String =
    accessibilityStatementHost + "/check-your-state-pension?referrerUrl=" + RedirectUrl(frontendHost + relativeReferrerPath).get(UnsafePermitAll).encodedUrl

  val gracePeriodStartMonth: Int              = getConfInt("gracePeriod.start.month", 4)
  val gracePeriodStartDay: Int                = getConfInt("gracePeriod.start.day", 5)
  val gracePeriodEndMonth: Int                = getConfInt("gracePeriod.end.month", 5)
  val gracePeriodEndDay: Int                  = getConfInt("gracePeriod.end.day", 6)

}
