/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.nisp.controllers.auth.AuthorisedForNisp
import uk.gov.hmrc.nisp.controllers.connectors.AuthenticationConnectors
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.views.html._

object ExclusionController extends ExclusionController with AuthenticationConnectors with PartialRetriever {
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override val applicationConfig: ApplicationConfig = ApplicationConfig
  override val statePensionService: StatePensionService =
    if (applicationConfig.useStatePensionAPI) StatePensionService else NispStatePensionService
  override val nationalInsuranceService: NationalInsuranceService =
    if (applicationConfig.useNationalInsuranceAPI) NationalInsuranceService else NispNationalInsuranceService
}

trait ExclusionController extends NispFrontendController with AuthorisedForNisp {

  val statePensionService: StatePensionService
  val nationalInsuranceService: NationalInsuranceService

  def showSP: Action[AnyContent] = AuthorisedByAny.async { implicit user => implicit request =>

    val statePensionF = statePensionService.getSummary(user.nino)
    val nationalInsuranceF = nationalInsuranceService.getSummary(user.nino)

    for(
      statePension <- statePensionF;
      nationalInsurance <- nationalInsuranceF
    ) yield {
      statePension match {
        case Left(exclusion) =>
          if (exclusion.exclusion == Exclusion.Dead)
            Ok(excluded_dead(Exclusion.Dead, exclusion.pensionAge))
          else if (exclusion.exclusion == Exclusion.ManualCorrespondenceIndicator)
            Ok(excluded_mci(Exclusion.ManualCorrespondenceIndicator, exclusion.pensionAge))
          else {
            Ok(excluded_sp(exclusion.exclusion, exclusion.pensionAge, exclusion.pensionDate, nationalInsurance.isRight))
          }
        case _ =>
          Logger.warn("User accessed /exclusion as non-excluded user")
          Redirect(routes.StatePensionController.show())
      }
    }
  }

  def showNI: Action[AnyContent] = AuthorisedByAny.async { implicit user => implicit request =>
     nationalInsuranceService.getSummary(user.nino).map {
        case Left(exclusion) =>
          if(exclusion == Exclusion.Dead) {
            Ok(excluded_dead(Exclusion.Dead, None))
          }
          else if(exclusion == Exclusion.ManualCorrespondenceIndicator) {
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
