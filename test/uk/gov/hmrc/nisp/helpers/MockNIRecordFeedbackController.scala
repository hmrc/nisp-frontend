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

package uk.gov.hmrc.nisp.helpers

import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.NIRecordFeedbackController
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.services.{NpsAvailabilityChecker, CitizenDetailsService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

object MockNIRecordFeedbackController extends NIRecordFeedbackController {
  override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
  override val nispConnector: NispConnector = MockNispConnector
  override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
  override val npsAvailabilityChecker: NpsAvailabilityChecker = MockNpsAvailabilityChecker

  override protected def authConnector: AuthConnector = MockAuthConnector
}
