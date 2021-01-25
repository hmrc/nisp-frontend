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

//TODO remove this once tests are passing
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.controllers.ExclusionController
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, ExcludedAuthAction}
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

//class MockExclusionController(nino: Nino) extends ExclusionController {
//  override val authenticate: ExcludedAuthAction = new FakeExcludedAuthAction(nino)
//  override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
//  override val statePensionService: StatePensionService = MockStatePensionServiceViaStatePension
//  override val nationalInsuranceService: NationalInsuranceService = MockNationalInsuranceServiceViaNationalInsurance
//  override implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer
//}
