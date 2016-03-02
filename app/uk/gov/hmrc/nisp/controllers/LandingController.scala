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

package uk.gov.hmrc.nisp.controllers

import play.api.mvc.{Result, Action, AnyContent}
import play.twirl.api.Html
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.IdentityVerificationConnector
import uk.gov.hmrc.nisp.controllers.auth.AuthorisedForNisp
import uk.gov.hmrc.nisp.controllers.connectors.AuthenticationConnectors
import uk.gov.hmrc.nisp.models.enums.IdentityVerificationResult
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, NpsAvailabilityChecker}
import uk.gov.hmrc.nisp.views.html.{identity_verification_landing, landing}
import uk.gov.hmrc.nisp.views.html.iv.failurepages.{not_authorised, technical_issue, locked_out, timeout}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}

import scala.concurrent.Future

object LandingController extends LandingController with AuthenticationConnectors {
  override val npsAvailabilityChecker: NpsAvailabilityChecker = NpsAvailabilityChecker
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override val applicationConfig: ApplicationConfig = ApplicationConfig
  override val identityVerificationConnector: IdentityVerificationConnector = IdentityVerificationConnector
}

trait LandingController extends FrontendController with Actions with AuthorisedForNisp {
  val npsAvailabilityChecker: NpsAvailabilityChecker
  val identityVerificationConnector: IdentityVerificationConnector

  def show: Action[AnyContent] = UnauthorisedAction(
    implicit request =>
      if(applicationConfig.identityVerification) {
        Ok(identity_verification_landing()).withNewSession
      } else {
        Ok(landing()).withNewSession
      }
  )

  def showNpsUnavailable: Action[AnyContent] = UnauthorisedAction(implicit request => ServiceUnavailable(uk.gov.hmrc.nisp.views.html.npsUnavailable()))

  def verifySignIn: Action[AnyContent] = AuthorisedByVerify { implicit user => implicit request =>
    Redirect(routes.AccountController.show())
  }

  def showNotAuthorised(journeyId: Option[String]) : Action[AnyContent] = UnauthorisedAction.async {implicit request =>
    val result = journeyId map { id =>
      val identityVerificationResult = identityVerificationConnector.identityVerificationResponse(id)
      identityVerificationResult map {
        case IdentityVerificationResult.FailedMatching => not_authorised()
        case IdentityVerificationResult.InsufficientEvidence => not_authorised()
        case IdentityVerificationResult.TechnicalIssue => technical_issue()
        case IdentityVerificationResult.LockedOut => locked_out()
        case IdentityVerificationResult.Timeout => timeout()
        case IdentityVerificationResult.Incomplete => not_authorised()
        case IdentityVerificationResult.PreconditionFailed => not_authorised()
        case IdentityVerificationResult.UserAborted => not_authorised()
      }
    } getOrElse Future.successful(not_authorised())

    result.map {
      Ok(_).withNewSession
    }
  }
}
