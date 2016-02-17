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

import play.api.Logger
import play.api.mvc.{AnyContent, Action}
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.auth.AuthorisedForNisp
import uk.gov.hmrc.nisp.controllers.connectors.AuthenticationConnectors
import uk.gov.hmrc.nisp.models.{SPExclusionsModel, SPResponseModel}
import uk.gov.hmrc.nisp.services.{NpsAvailabilityChecker, CitizenDetailsService}
import uk.gov.hmrc.nisp.views.html.excluded
import uk.gov.hmrc.play.frontend.controller.FrontendController

object ExclusionController extends ExclusionController with AuthenticationConnectors {
  override val nispConnector: NispConnector = NispConnector
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override val npsAvailabilityChecker: NpsAvailabilityChecker = NpsAvailabilityChecker
}

trait ExclusionController extends FrontendController with AuthorisedForNisp {
  val nispConnector: NispConnector

  def show: Action[AnyContent] = AuthorisedByVerify.async { implicit user => implicit request =>
    val nino = user.nino.getOrElse("")
    nispConnector.connectToGetSPResponse(nino).map{
      case SPResponseModel(_, Some(spExclusions: SPExclusionsModel)) => Ok(excluded(nino, spExclusions, user))
      case _ =>
        Logger.warn("User accessed /exclusion as non-excluded user")
        Redirect(routes.AccountController.show())
    }
  }
}
