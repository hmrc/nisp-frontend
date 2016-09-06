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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.auth.AuthorisedForNisp
import uk.gov.hmrc.nisp.controllers.connectors.AuthenticationConnectors
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{CitizenDetailsService}
import uk.gov.hmrc.nisp.views.html.{excluded_ni, excluded_sp, excluded_sp_old, excluded_mci}
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.play.http.SessionKeys

object ExclusionController extends ExclusionController with AuthenticationConnectors with PartialRetriever {
  override val nispConnector: NispConnector = NispConnector
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override val applicationConfig: ApplicationConfig = ApplicationConfig
}

trait ExclusionController extends NispFrontendController with AuthorisedForNisp {
  val nispConnector: NispConnector

  def showSP: Action[AnyContent] = AuthorisedByAny.async { implicit user => implicit request =>
    val nino = user.nino.getOrElse("")

    nispConnector.connectToGetSPResponse(nino).map {
      case SPResponseModel(Some(spSummary: SPSummaryModel), Some(spExclusions: ExclusionsModel), niExclusionOption, Some(schemeMembership: SchemeMembershipModel)) =>
        if (!spExclusions.exclusions.contains(Exclusion.Dead)) {
          if(spExclusions.exclusions.contains(Exclusion.ManualCorrespondenceIndicator)) {
            Ok(excluded_mci(spExclusions.exclusions, Some(spSummary.statePensionAge)))
          } else {
            Ok(excluded_sp(
              spExclusions.exclusions,
              spSummary.statePensionAge,
              niExclusionOption.fold(true)(_.exclusions.isEmpty)
            ))
          }
        } else {
          Ok(excluded_sp_old(nino, spExclusions, spSummary.statePensionAge))
        }
      case _ =>
        Logger.warn("User accessed /exclusion as non-excluded user")
        Redirect(routes.AccountController.show())
    }
  }

  def showNI: Action[AnyContent] = AuthorisedByAny.async { implicit user => implicit request =>
    val nino = user.nino.getOrElse("")
     nispConnector.connectToGetNIResponse(nino).map {
        case NIResponse(_, _, Some(niExclusions: ExclusionsModel)) =>
          if(niExclusions.exclusions.contains(Exclusion.ManualCorrespondenceIndicator)) {
            Ok(excluded_mci(niExclusions.exclusions, None))
          } else {
            Ok(excluded_ni(nino, niExclusions))
          }
        case _ =>
          Logger.warn("User accessed /exclusion/nirecord as non-excluded user")
          Redirect(routes.NIRecordController.showGaps())
    }
  }
}
