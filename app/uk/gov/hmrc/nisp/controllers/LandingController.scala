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

package uk.gov.hmrc.nisp.controllers

import com.google.inject.Inject
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.{IdentityVerificationConnector, IdentityVerificationSuccessResponse}
import uk.gov.hmrc.nisp.controllers.auth.VerifyAuthActionImpl
import uk.gov.hmrc.nisp.views.html.iv.failurepages.{locked_out, not_authorised, technical_issue, timeout}
import uk.gov.hmrc.nisp.views.html.{identity_verification_landing, landing}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.{ExecutionContext, Future}

class LandingController @Inject()(identityVerificationConnector: IdentityVerificationConnector,
                                  applicationConfig: ApplicationConfig,
                                  verifyAuthAction: VerifyAuthActionImpl,
                                  mcc: MessagesControllerComponents)
                                 (implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever,
                                  val formPartialRetriever: FormPartialRetriever,
                                  val templateRenderer: TemplateRenderer,
                                  val executor: ExecutionContext) extends NispFrontendController(mcc) with I18nSupport {

  val logger = Logger(this.getClass)

  def show: Action[AnyContent] = Action(
    implicit request =>
      if (applicationConfig.identityVerification) {
        Ok(identity_verification_landing()).withNewSession
      } else {
        Ok(landing()).withNewSession
      }
  )

  def verifySignIn: Action[AnyContent] = verifyAuthAction {
    implicit request =>
      Redirect(routes.StatePensionController.show())
  }

  def showNotAuthorised(journeyId: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    val result = journeyId map { id =>

      import IdentityVerificationSuccessResponse._

      val identityVerificationResult = identityVerificationConnector.identityVerificationResponse(id)
      identityVerificationResult map {
        case IdentityVerificationSuccessResponse(FailedMatching) => {
          logger.warn(s"identityVerificationConnector.identityVerificationResponse has returned, $FailedMatching error")
          Unauthorized(not_authorised())
        }
        case IdentityVerificationSuccessResponse(InsufficientEvidence) => {
          logger.warn(s"identityVerificationConnector.identityVerificationResponse has returned, $InsufficientEvidence error")
          Unauthorized(not_authorised())
        }
        case IdentityVerificationSuccessResponse(TechnicalIssue) => {
          logger.warn(s"identityVerificationConnector.identityVerificationResponse has returned, $TechnicalIssue error")
          InternalServerError(technical_issue())
        }
        case IdentityVerificationSuccessResponse(LockedOut) => {
          logger.warn(s"identityVerificationConnector.identityVerificationResponse has returned, $Locked error")
          Locked(locked_out())
        }
        case IdentityVerificationSuccessResponse(Timeout) => {
          logger.warn(s"identityVerificationConnector.identityVerificationResponse has returned, $Timeout error")
          Unauthorized(timeout())
        }
        case IdentityVerificationSuccessResponse(Incomplete) => {
          logger.warn(s"identityVerificationConnector.identityVerificationResponse has returned, $Incomplete error")
          Unauthorized(not_authorised())
        }
        case IdentityVerificationSuccessResponse(IdentityVerificationSuccessResponse.PreconditionFailed) => {
          logger.warn(s"identityVerificationConnector.identityVerificationResponse has returned, ${IdentityVerificationSuccessResponse.PreconditionFailed} error")
          Unauthorized(not_authorised())
        }
        case IdentityVerificationSuccessResponse(UserAborted) => {
          logger.warn(s"identityVerificationConnector.identityVerificationResponse has returned, $UserAborted error")
          Unauthorized(not_authorised())
        }
        case IdentityVerificationSuccessResponse(FailedIV) => {
          logger.warn(s"identityVerificationConnector.identityVerificationResponse has returned, $FailedIV error")
          Unauthorized(not_authorised())
        }
        case response => logger.warn(s"Unhandled Response from Identity Verification: $response"); InternalServerError(technical_issue())
      }
    } getOrElse Future.successful(Unauthorized(not_authorised(showFirstParagraph = false)))

    result.map(_.withNewSession)
  }
}
