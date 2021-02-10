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

import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import play.api.{Logger, Play}
import uk.gov.hmrc.nisp.config.wiring.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, ExcludedAuthAction, ExcludedAuthActionImpl}
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.views.html._

object ExclusionController extends ExclusionController with PartialRetriever with NispFrontendController {
  override val statePensionService: StatePensionService = StatePensionService
  override val authenticate: ExcludedAuthActionImpl = Play.current.injector.instanceOf[ExcludedAuthActionImpl]
  override val nationalInsuranceService: NationalInsuranceService = NationalInsuranceService
}

trait ExclusionController extends NispFrontendController {

  val statePensionService: StatePensionService
  val nationalInsuranceService: NationalInsuranceService
  val authenticate: ExcludedAuthAction

  def showSP: Action[AnyContent] = authenticate.async {
    implicit request =>
      implicit val authDetails: AuthDetails = request.authDetails

      val statePensionF = statePensionService.getSummary(request.nino)
      val nationalInsuranceF = nationalInsuranceService.getSummary(request.nino)

      for (
        statePension <- statePensionF;
        nationalInsurance <- nationalInsuranceF
      ) yield {
        statePension match {
          case Right(sp) if sp.reducedRateElection =>
            Ok(excluded_sp(Exclusion.MarriedWomenReducedRateElection, Some(sp.pensionAge), Some(sp.pensionDate), false, None))
          case Left(exclusion) =>
            if (exclusion.exclusion == Exclusion.Dead)
              Ok(excluded_dead(Exclusion.Dead, exclusion.pensionAge))
            else if (exclusion.exclusion == Exclusion.ManualCorrespondenceIndicator)
              Ok(excluded_mci(Exclusion.ManualCorrespondenceIndicator, exclusion.pensionAge))
            else {
              Ok(excluded_sp(exclusion.exclusion, exclusion.pensionAge, exclusion.pensionDate, nationalInsurance.isRight, exclusion.statePensionAgeUnderConsideration))
            }
          case _ =>
            Logger.warn("User accessed /exclusion as non-excluded user")
            Redirect(routes.StatePensionController.show())
        }
      }
  }

  def showNI: Action[AnyContent] = authenticate.async {
    implicit request =>
      implicit val authDetails: AuthDetails = request.authDetails
      nationalInsuranceService.getSummary(request.nino).map {
        case Left(exclusion) =>
          if (exclusion == Exclusion.Dead) {
            Ok(excluded_dead(Exclusion.Dead, None))
          }
          else if (exclusion == Exclusion.ManualCorrespondenceIndicator) {
            Ok(excluded_mci(Exclusion.ManualCorrespondenceIndicator, None))
          } else {
            Ok(excluded_ni(exclusion))
          }
        case _ =>
          Logger.warn("User accessed /exclusion/nirecord as non-excluded user")
          Redirect(routes.NIRecordController.showGaps())
      }
  }
}
