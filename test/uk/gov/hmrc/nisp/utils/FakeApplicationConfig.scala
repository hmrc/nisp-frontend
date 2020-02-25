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