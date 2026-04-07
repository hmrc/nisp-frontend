/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.StandardAuthJourney
import uk.gov.hmrc.nisp.models.PayableGapInfo
import uk.gov.hmrc.nisp.views.html.gracePeriod
import uk.gov.hmrc.time.CurrentTaxYear

import java.time.{Clock, LocalDate}
import scala.concurrent.ExecutionContext

class GracePeriodController @Inject()(
  authenticate: StandardAuthJourney,
  mcc: MessagesControllerComponents,
  gracePeriodView: gracePeriod,
  clock: Clock,
  appConfig: ApplicationConfig
)(
  implicit val executor: ExecutionContext
) extends NispFrontendController(mcc)
    with I18nSupport
    with CurrentTaxYear {

  override def now: () => LocalDate = () => LocalDate.now(clock)

  def showGracePeriod: Action[AnyContent] =
    authenticate.pertaxAuthActionWithGracePeriod { implicit request =>
      Ok(gracePeriodView(current, PayableGapInfo(appConfig.niRecordPayableYears)))
    }
}
