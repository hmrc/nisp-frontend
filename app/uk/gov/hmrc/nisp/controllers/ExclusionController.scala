/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.{Logger, Play}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.AuthorisedForNisp
import uk.gov.hmrc.nisp.controllers.connectors.AuthenticationConnectors
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.views.html._

object ExclusionController extends ExclusionController with PartialRetriever with NispFrontendController {
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override val applicationConfig: ApplicationConfig = ApplicationConfig
  override val statePensionService: StatePensionService = StatePensionService
  override val authenticate: AuthAction = Play.current.injector.instanceOf[AuthAction]
  override val nationalInsuranceService: NationalInsuranceService = NationalInsuranceService
}

trait ExclusionController extends NispFrontendController {

  val statePensionService: StatePensionService
  val nationalInsuranceService: NationalInsuranceService
  val authenticate: AuthAction

  def showSP: Action[AnyContent] = authenticate.async {
    implicit request =>

      implicit val user = request.nispAuthedUser
      val statePensionF = statePensionService.getSummary(user.nino)
      val nationalInsuranceF = nationalInsuranceService.getSummary(user.nino)

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
      implicit val user = request.nispAuthedUser
      nationalInsuranceService.getSummary(user.nino).map {
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
