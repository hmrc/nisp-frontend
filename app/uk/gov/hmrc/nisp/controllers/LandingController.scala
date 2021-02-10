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

import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.{CitizenDetailsConnector, IdentityVerificationConnector, NispAuthConnector}
import uk.gov.hmrc.nisp.connectors.{IdentityVerificationConnector, IdentityVerificationSuccessResponse}
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, VerifyAuthActionImpl}
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.nisp.views.html.iv.failurepages.{locked_out, not_authorised, technical_issue, timeout}
import uk.gov.hmrc.nisp.views.html.{identity_verification_landing, landing}
import uk.gov.hmrc.play.frontend.controller.UnauthorisedAction

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

object LandingController extends LandingController with PartialRetriever {
  override val applicationConfig: ApplicationConfig = ApplicationConfig
  override val identityVerificationConnector: IdentityVerificationConnector = IdentityVerificationConnector
  override val verifyAuthAction: AuthAction = new VerifyAuthActionImpl(
    new NispAuthConnector,
    new CitizenDetailsService(CitizenDetailsConnector))
}

trait LandingController extends NispFrontendController {
  val identityVerificationConnector: IdentityVerificationConnector
  val applicationConfig: ApplicationConfig
  val verifyAuthAction: AuthAction

  def show: Action[AnyContent] = UnauthorisedAction(
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

  def showNotAuthorised(journeyId: Option[String]): Action[AnyContent] = UnauthorisedAction.async { implicit request =>
    val result = journeyId map { id =>

      import IdentityVerificationSuccessResponse._

      val identityVerificationResult = identityVerificationConnector.identityVerificationResponse(id)
      identityVerificationResult map {
        case IdentityVerificationSuccessResponse(FailedMatching) => not_authorised()
        case IdentityVerificationSuccessResponse(InsufficientEvidence) => not_authorised()
        case IdentityVerificationSuccessResponse(TechnicalIssue) => technical_issue()
        case IdentityVerificationSuccessResponse(LockedOut) => locked_out()
        case IdentityVerificationSuccessResponse(Timeout) => timeout()
        case IdentityVerificationSuccessResponse(Incomplete) => not_authorised()
        case IdentityVerificationSuccessResponse(IdentityVerificationSuccessResponse.PreconditionFailed) => not_authorised()
        case IdentityVerificationSuccessResponse(UserAborted) => not_authorised()
        case IdentityVerificationSuccessResponse(FailedIV) => not_authorised()
        case response => Logger.warn(s"Unhandled Response from Identity Verification: $response"); technical_issue()
      }
    } getOrElse Future.successful(not_authorised(showFirstParagraph = false))

    result.map {
      Ok(_).withNewSession
    }
  }
}
