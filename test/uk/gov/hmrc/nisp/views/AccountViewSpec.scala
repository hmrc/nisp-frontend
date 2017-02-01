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

package uk.gov.hmrc.nisp.views.html

import java.util.UUID

import org.joda.time.{LocalDate, LocalDateTime}
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.{SPAmountModel, StatePensionAmount, StatePensionAmountRegular, StatePensionExclusion}
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService, StatePensionService}
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, SessionKeys, Upstream4xxResponse}
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils.now
import uk.gov.hmrc.nisp.views.html.HtmlSpec
import uk.gov.hmrc.nisp.views._
import uk.gov.hmrc.nisp.controllers._
import play.twirl.api.Html
import uk.gov.hmrc.nisp.models.enums.Exclusion

import scala.concurrent.Future


class AccountViewSpec extends UnitSpec with MockitoSugar with HtmlSpec with BeforeAndAfter with OneAppPerSuite {

  val mockUserNino = TestAccountBuilder.regularNino;
  val mockUserIdForecastOnly =  "/auth/oid/mockforecastonly"
  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername

  private def authenticatedFakeRequest(userId: String = mockUserId) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId,
    SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
  )

  implicit val headerCarrier = HeaderCarrier()


  "Account page template" should {
    "render Account Page template" in {
//
//      val controller = new AccountController {
//        override def nispConnector: NispConnector = mock[NispConnector]
//
//        override def statePensionService: StatePensionService = mock[StatePensionService]
//
//        override lazy val customAuditConnector: CustomAuditConnector = ???
//        override lazy val applicationConfig: ApplicationConfig = ???
//        override lazy val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
//
//        override protected def authConnector: AuthConnector = MockAuthConnector
//
//        override val sessionCache: SessionCache = MockSessionCache
//        override val metricsService: MetricsService = MockMetricsService
//        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
//      }
//
//      when(controller.statePensionService.getSummary(Matchers.any()))
//        .thenReturn(Future.successful(Left(StatePensionExclusion(List(Exclusion.Dead)))))
//
//
//      var sResult = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly)) ;//AccountController.show(fakeRequest);
//      val htmlAccountDoc = asDocument(contentAsString(sResult));
//      assertElementContainsText(htmlAccountDoc ,"div.helpline-sidebar>h2" , "Get help")

    }

  }

}
