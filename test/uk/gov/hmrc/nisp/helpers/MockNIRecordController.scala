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

package uk.gov.hmrc.nisp.helpers

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationGlobalTrait
import uk.gov.hmrc.nisp.controllers.NIRecordController
import uk.gov.hmrc.nisp.controllers.auth.AuthAction
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.services.{MetricsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

class MockNIRecordControllerImpl(nino: Nino) extends MockNIRecordController {
  override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
  override val sessionCache: SessionCache = MockSessionCache
  override lazy val showFullNI: Boolean = true
  override val currentDate = new LocalDate(2016,9,9)
  override val metricsService: MetricsService = MockMetricsService
  override val authenticate: AuthAction = new MockAuthAction(nino)
}

trait MockNIRecordController extends NIRecordController {
  override val nationalInsuranceService: NationalInsuranceService = MockNationalInsuranceServiceViaNationalInsurance
  override val statePensionService: StatePensionService = MockStatePensionServiceViaStatePension

  override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  override implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer
  override val applicationGlobal:ApplicationGlobalTrait = MockApplicationGlobal

}
