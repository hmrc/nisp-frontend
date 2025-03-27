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
import uk.gov.hmrc.nisp.models.PayableGapExtensionDetails
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{RedirectUrl, UnsafePermitAll}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class ApplicationConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {
  import servicesConfig._

  lazy private val contactHost            = servicesConfig.getString("contact-frontend.host")

  lazy val appName: String                = getString("appName")

  lazy val serviceUrl: String            = servicesConfig.getString("serviceUrl")
  lazy val contactFormServiceIdentifier  = "NISP"
  lazy val reportAProblemNonJSUrl        = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  lazy val postSignInRedirectUrl: String     = getString("login-callback.url")
  lazy val notAuthorisedRedirectUrl: String  = getString("not-authorised-callback.url")
  lazy val ivUpliftUrl: String               = getString("identity-verification-uplift.host")
  lazy val mfaUpliftUrl: String              = getString("mfa-uplift.url")
  lazy val ggSignInUrl: String               = getString("government-gateway-sign-in.host")

  lazy val showExcessiveTrafficMessage: Boolean  = getBoolean("excessiveTrafficToggle")
  lazy val isleManLink: String                   = getString("isle-man-link.url")
  lazy val citizenAdviceLinkEn: String           = getString("citizens-advice-link-en.url")
  lazy val citizenAdviceLinkCy: String           = getString("citizens-advice-link-cy.url")
  lazy val moneyAdviceLinkEn: String             = getString("money-advice-link-en.url")
  lazy val moneyAdviceLinkCy: String             = getString("money-advice-link-cy.url")
  lazy val pensionWiseLink: String               = getString("pension-wise-link.url")
  lazy val pensionWiseLinkCy: String             = getString("pension-wise-link-cy.url")

  lazy val futurePensionLink: String             = getString("govUkLinks.future-pension-link.url")
  lazy val nationalInsuranceLink: String         = getString("govUkLinks.national-insurance-link.url")
  lazy val niHowMuchYouPayLink: String           = getString("govUkLinks.ni-how-much-you-pay-link.url")
  lazy val nationalInsuranceCreditLink: String   = getString("govUkLinks.national-insurance-credits-link.url")
  lazy val additionalStatePensionLink: String    = getString("govUkLinks.additional-state-pension-link.url")
  lazy val pensionDeferralLink: String           = getString("govUkLinks.pension-deferral-link.url")
  lazy val newStatePensionLink: String           = getString("govUkLinks.new-state-pension-link.url")
  lazy val copeLink: String                      = getString("govUkLinks.contracted-out-pension-link.url")
  lazy val homeResponsibilitiesLink: String      = getString("govUkLinks.home-responsibilities-protection-link.url")
  lazy val pensionCreditLink: String             = getString("govUkLinks.pension-credit-link.url")
  lazy val abroadLink: String                    = getString("govUkLinks.living-and-working-overseas-link.url")

  lazy private val frontendHost                  = getString("nisp-frontend.host")
  lazy private val accessibilityStatementHost: String    = getString("accessibility-statement.url") + "/accessibility-statement"

  lazy val showFullNI: Boolean                   = getConfBool("features.fullNIrecord", false)
  lazy val futureProofPersonalMax: Boolean       = getConfBool("features.future-proof.personalMax", false)
  lazy val isWelshEnabled: Boolean               = getConfBool("features.welsh-translation", false)
  lazy val feedbackFrontendUrl: String           = getString("feedback-frontend.url")
  lazy val niRecordPayableYears: Int             = getInt("numberOfPayableTaxYears")
  lazy val friendlyUsers: Seq[String]            = config.get[Seq[String]]("allowedUsers.friendly")
  lazy val allowedUsersEndOfNino: Seq[String]    = config.get[Seq[String]]("allowedUsers.endOfNino")
  lazy val nispModellingFrontendUrl: String      = getString("nisp-modelling.url")


  lazy val citizenDetailsServiceUrl: String       = baseUrl("citizen-details")
  lazy val identityVerificationServiceUrl: String = baseUrl("identity-verification")
  lazy val nationalInsuranceServiceUrl: String    = baseUrl("national-insurance")
  lazy val statePensionServiceUrl: String         = baseUrl("state-pension")

  lazy val pertaxAuthBaseUrl: String = baseUrl("pertax-auth")

  def accessibilityStatementUrl(relativeReferrerPath: String): String =
    accessibilityStatementHost + "/check-your-state-pension?referrerUrl=" + RedirectUrl(frontendHost + relativeReferrerPath).get(UnsafePermitAll).encodedUrl


  lazy val internalAuthResourceType: String = getConfString("internal-auth.resource-type", "ddcn-live-admin-frontend")

  lazy val payableGapExtensions: Seq[PayableGapExtensionDetails] = config.get[Seq[PayableGapExtensionDetails]]("payableGapsExtensions.extensions")
  lazy val payableGapDefault: Int = getInt("payableGapsExtensions.defaults.payableGaps")
}
