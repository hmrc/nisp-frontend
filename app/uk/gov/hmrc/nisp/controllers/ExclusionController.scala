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
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, ExcludedAuthAction}
import uk.gov.hmrc.nisp.models.Exclusion._
import uk.gov.hmrc.nisp.models.{Exclusion, StatePensionExclusionFiltered, StatePensionExclusionFilteredWithCopeDate}
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.views.html._
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.ExecutionContext

class ExclusionController @Inject()(statePensionService: StatePensionService,
                                    nationalInsuranceService: NationalInsuranceService,
                                    authenticate: ExcludedAuthAction,
                                    mcc: MessagesControllerComponents,
                                    excludedCopeView: excluded_cope,
                                    excludedCopeExtendedView: excluded_cope_extended,
                                    excludedCopeFailedView: excluded_cope_failed,
                                    excludedSp: excluded_sp,
                                    excludedDead: excluded_dead,
                                    excludedMci: excluded_mci,
                                    excludedNi: excluded_ni)
                                   (implicit val executor: ExecutionContext,
                                    implicit val formPartialRetriever: FormPartialRetriever,
                                    val templateRenderer: TemplateRenderer,
                                    val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever)
  extends NispFrontendController(mcc) with I18nSupport with Logging {

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
            Ok(excludedSp(MarriedWomenReducedRateElection, Some(sp.pensionAge), Some(sp.pensionDate), canSeeNIRecord = false, None))
          case Left(statePensionExclusionFiltered: StatePensionExclusionFiltered) =>
            if (statePensionExclusionFiltered.exclusion == Dead)
              Ok(excludedDead(Exclusion.Dead, statePensionExclusionFiltered.pensionAge))
            else if (statePensionExclusionFiltered.exclusion == ManualCorrespondenceIndicator)
              Ok(excludedMci(Exclusion.ManualCorrespondenceIndicator, statePensionExclusionFiltered.pensionAge))
            else if (statePensionExclusionFiltered.exclusion == CopeProcessingFailed)
              Ok(excludedCopeFailedView())
            else
              Ok(excludedSp(
                statePensionExclusionFiltered.exclusion,
                statePensionExclusionFiltered.pensionAge,
                statePensionExclusionFiltered.pensionDate,
                nationalInsurance.isRight,
                statePensionExclusionFiltered.statePensionAgeUnderConsideration
              ))
          case Left(spExclusion: StatePensionExclusionFilteredWithCopeDate) =>
            if(spExclusion.previousAvailableDate.exists(_.isBefore(spExclusion.copeAvailableDate))) {
              Ok(excludedCopeExtendedView(spExclusion.copeAvailableDate))
            } else {
              Ok(excludedCopeView(spExclusion.copeAvailableDate))
            }
          case _ =>
            logger.warn("User accessed/exclusion as non-excluded user")
            Redirect(routes.StatePensionController.show)
        }
      }
  }

  def showNI: Action[AnyContent] = authenticate.async {
    implicit request =>
      implicit val authDetails: AuthDetails = request.authDetails
      nationalInsuranceService.getSummary(request.nino).map {
        case Left(CopeProcessing) | Left(CopeProcessingFailed) =>
          Redirect(routes.ExclusionController.showSP)
        case Left(exclusion) =>
          if (exclusion == Dead) {
            Ok(excludedDead(Exclusion.Dead, None))
          }
          else if (exclusion == Exclusion.ManualCorrespondenceIndicator) {
            Ok(excludedMci(ManualCorrespondenceIndicator, None))
          } else {
            Ok(excludedNi(exclusion))
          }
        case _ =>
          logger.warn("User accessed /exclusion/nirecord as non-excluded user")
          Redirect(routes.NIRecordController.showGaps)
      }
  }
}
