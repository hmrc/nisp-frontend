/*
 * Copyright 2024 HM Revenue & Customs
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

import scala.concurrent.{ExecutionContext, Future}

class ExclusionController @Inject()(
  statePensionService: StatePensionService,
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
  errorHandler: ErrorHandler
)(
  implicit val executor: ExecutionContext
) extends NispFrontendController(mcc)
  with I18nSupport
  with Logging {

  def showSP: Action[AnyContent] = authenticate.async { implicit request =>
    statePensionService.getSummary(request.nino) flatMap {
      case Right(Right(statePension)) if statePension.reducedRateElection =>
        Future.successful(Ok(excludedSp(
          MarriedWomenReducedRateElection,
          Some(statePension.pensionAge),
          Some(statePension.pensionDate),
          canSeeNIRecord = false,
          None)))
      case Right(Left(statePensionExclusionFiltered: StatePensionExclusionFiltered)) =>
        if (statePensionExclusionFiltered.exclusion == Dead)
          Future.successful(Ok(excludedDead(Exclusion.Dead, statePensionExclusionFiltered.pensionAge)))
        else if (statePensionExclusionFiltered.exclusion == ManualCorrespondenceIndicator)
          Future.successful(Ok(excludedMci(Exclusion.ManualCorrespondenceIndicator, statePensionExclusionFiltered.pensionAge)))
        else if (statePensionExclusionFiltered.exclusion == CopeProcessingFailed)
          Future.successful(Ok(excludedCopeFailedView()))
        else {
          nationalInsuranceService.getSummary(request.nino) map {
            nationalInsurance =>
              Ok(excludedSp(
                statePensionExclusionFiltered.exclusion,
                statePensionExclusionFiltered.pensionAge,
                statePensionExclusionFiltered.pensionDate,
                nationalInsurance.isRight,
                statePensionExclusionFiltered.statePensionAgeUnderConsideration
              ))
          }
        }
      case Right(Left(spExclusion: StatePensionExclusionFilteredWithCopeDate)) =>
        if(spExclusion.previousAvailableDate.exists(_.isBefore(spExclusion.copeDataAvailableDate))) {
          Future.successful(Ok(excludedCopeExtendedView(spExclusion.copeDataAvailableDate)))
        } else {
          Future.successful(Ok(excludedCopeView(spExclusion.copeDataAvailableDate)))
        }
      case Left(_) =>
        errorHandler.internalServerErrorTemplate.map(html => InternalServerError(html))
      case _ =>
        logger.warn("User accessed/exclusion as non-excluded user")
        Future.successful(Redirect(routes.StatePensionController.show))
    }
  }

  def showNI: Action[AnyContent] = authenticate.async {
    implicit request =>
      nationalInsuranceService.getSummary(request.nino).map {
        case Right(Left(StatePensionExclusionFilteredWithCopeDate(_, copeDataAvailableDate, previousAvailableDate)))
          if previousAvailableDate.isDefined =>
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
