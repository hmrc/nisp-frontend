/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.nisp.controllers.auth.ExcludedAuthAction
import uk.gov.hmrc.nisp.errorHandler.ErrorHandler
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
                                    excludedCopeView: excluded_cope_sp,
                                    excludedCopeExtendedView: excluded_cope_extended_sp,
                                    excludedCopeFailedView: excluded_cope_failed_sp,
                                    excludedSp: excluded_sp,
                                    excludedCopeNi: excluded_cope_ni,
                                    excludedCopeFailedNi: excluded_cope_failed_ni,
                                    excludedCopeExtendedNi: excluded_cope_extended_ni,
                                    excludedDead: excluded_dead,
                                    excludedMci: excluded_mci,
                                    excludedNi: excluded_ni,
                                    errorHandler: ErrorHandler)
                                   (implicit val executor: ExecutionContext,
                                    implicit val formPartialRetriever: FormPartialRetriever,
                                    val templateRenderer: TemplateRenderer,
                                    val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever)
  extends NispFrontendController(mcc) with I18nSupport with Logging {

  def showSP: Action[AnyContent] = authenticate.async { implicit request =>
    val statePensionF      = statePensionService.getSummary(request.nino)
    val nationalInsuranceF = nationalInsuranceService.getSummary(request.nino)

      for (
        statePension <- statePensionF;
        nationalInsurance <- nationalInsuranceF
      ) yield {
        statePension match {
          case Right(Right(sp)) if sp.reducedRateElection =>
            Ok(excludedSp(MarriedWomenReducedRateElection, Some(sp.pensionAge), Some(sp.pensionDate), canSeeNIRecord = false, None))
          case Right(Left(statePensionExclusionFiltered: StatePensionExclusionFiltered)) =>
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
          case Right(Left(spExclusion: StatePensionExclusionFilteredWithCopeDate)) =>
            if(spExclusion.previousAvailableDate.exists(_.isBefore(spExclusion.copeDataAvailableDate))) {
              Ok(excludedCopeExtendedView(spExclusion.copeDataAvailableDate))
            } else {
              Ok(excludedCopeView(spExclusion.copeDataAvailableDate))
            }
          case Left(error) => InternalServerError(errorHandler.internalServerErrorTemplate)
          case _ =>
            logger.warn("User accessed/exclusion as non-excluded user")
            Redirect(routes.StatePensionController.show)
        }
      }
  }

  def showNI: Action[AnyContent] = authenticate.async {
    implicit request =>
      nationalInsuranceService.getSummary(request.nino).map {
        case Right(Left(StatePensionExclusionFilteredWithCopeDate(_, copeDataAvailableDate, previousAvailableDate)))
          if (previousAvailableDate.isDefined) =>
          Ok(excludedCopeExtendedNi(copeDataAvailableDate))
        case Right(Left(StatePensionExclusionFilteredWithCopeDate(_, copeDataAvailableDate, _))) =>
          Ok(excludedCopeNi(copeDataAvailableDate))
        case Right(Left(StatePensionExclusionFiltered(Exclusion.CopeProcessingFailed, _, _, _))) =>
          Ok(excludedCopeFailedNi())
        case Right(Left(StatePensionExclusionFiltered(Exclusion.Dead, _, _, _))) =>
          Ok(excludedDead(Exclusion.Dead, None))
        case Right(Left(StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator, _, _, _))) =>
          Ok(excludedMci(ManualCorrespondenceIndicator, None))
        case Right(Left(StatePensionExclusionFiltered(exclusion, _, _, _))) =>
          Ok(excludedNi(exclusion))
        case _ =>
          logger.warn("User accessed /exclusion/nirecord as non-excluded user")
          Redirect(routes.NIRecordController.showGaps)
      }
  }
}
