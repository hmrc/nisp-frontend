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
import play.api.{Application, Logger}
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.ExecutionContext
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, ExcludedAuthAction}
import uk.gov.hmrc.nisp.models.Exclusion
import uk.gov.hmrc.nisp.models.Exclusion._
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.views.html._
import uk.gov.hmrc.nisp.utils.DateProvider

class ExclusionController @Inject()(statePensionService: StatePensionService,
                                    nationalInsuranceService: NationalInsuranceService,
                                    authenticate: ExcludedAuthAction,
                                    mcc: MessagesControllerComponents)
                                   (implicit val executor: ExecutionContext,
                                    val formPartialRetriever: FormPartialRetriever,
                                    val templateRenderer: TemplateRenderer,
                                    val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever)
  extends NispFrontendController(mcc) with I18nSupport{

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
            Ok(excluded_sp(MarriedWomenReducedRateElection, Some(sp.pensionAge), Some(sp.pensionDate), false, None))
          case Left(statePensionExclFiltered) =>
            if (statePensionExclFiltered.exclusion == Dead)
              Ok(excluded_dead(Exclusion.Dead, statePensionExclFiltered.pensionAge))
            else if (statePensionExclFiltered.exclusion == ManualCorrespondenceIndicator)
              Ok(excluded_mci(Exclusion.ManualCorrespondenceIndicator, statePensionExclFiltered.pensionAge))
            else if (statePensionExclFiltered.exclusion == CopeProcessing)
              Ok(excluded_cope(CopeProcessing, statePensionExclFiltered.pensionAge, statePensionExclFiltered.copeDataAvailableDate))
            else if (statePensionExclFiltered.exclusion == CopeProcessingFailed)
              Ok(excluded_cope(CopeProcessingFailed, statePensionExclFiltered.pensionAge, None))
            else
              Ok(excluded_sp(statePensionExclFiltered.exclusion, statePensionExclFiltered.pensionAge, statePensionExclFiltered.pensionDate, nationalInsurance.isRight, statePensionExclFiltered.statePensionAgeUnderConsideration))
          case exclusion =>
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
          if (exclusion == Dead) {
            Ok(excluded_dead(Exclusion.Dead, None))
          }
          else if (exclusion == Exclusion.ManualCorrespondenceIndicator) {
            Ok(excluded_mci(ManualCorrespondenceIndicator, None))
          } else {
            Ok(excluded_ni(exclusion))
          }
        case _ =>
          Logger.warn("User accessed /exclusion/nirecord as non-excluded user")
          Redirect(routes.NIRecordController.showGaps())
      }
  }
}
