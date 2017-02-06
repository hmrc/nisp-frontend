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

package uk.gov.hmrc.nisp.views

import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers._
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService, StatePensionService}
import uk.gov.hmrc.nisp.views.html.HtmlSpec
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec



class VoluntaryContributionsViewSpec extends UnitSpec with MockitoSugar with HtmlSpec with BeforeAndAfter with OneAppPerSuite {


  implicit val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever

  val mockUserNino = TestAccountBuilder.regularNino;
  val mockUserIdForecastOnly =  "/auth/oid/mockforecastonly"
  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername

  lazy val fakeRequest = FakeRequest();

  val controller = new AccountController {
    override def nispConnector: NispConnector = mock[NispConnector]

    override def statePensionService: StatePensionService = mock[StatePensionService]

    override lazy val customAuditConnector: CustomAuditConnector = ???
    override lazy val applicationConfig: ApplicationConfig = ???
    override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService

    override protected def authConnector: AuthConnector = MockAuthConnector

    override val sessionCache: SessionCache = MockSessionCache
    override val metricsService: MetricsService = MockMetricsService
    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  }

  "Voluntary contributions view" should {

    lazy val sResult = html.nirecordVoluntaryContributions()
    lazy val htmlAccountDoc = asDocument(contentAsString(sResult))

    "render page with heading  Voluntary contributions " in {

      assertEqualsMessage(htmlAccountDoc, "h1.heading-large", "nisp.nirecord.voluntarycontributions.heading")
    }
    "render page with text  Before considering paying voluntary contributions you should" in {
      assertEqualsMessage(htmlAccountDoc, "p.lede", "nisp.nirecord.voluntarycontributions.title.message")
    }
    "render page with text  '1.  Check if you may get extra pension income because you' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > h2.heading-medium:nth-child(3)", "nisp.nirecord.voluntarycontributions.h2title.1")
    }
    "render page with text  'inherit or increase it from a partner' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(4)>summary>span.summary", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle1")
    }
    "render page with text  'Your amount may change if you:' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(4)>div.panel-indent>p", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle1.heading")
    }
    "render page with text  'are widowed, divorced or have dissolved your civil partnership' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(4)>div.panel-indent>ul.list-bullet>li:nth-child(1)", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle1.message1")
    }
    "render page with text  'paid married women’s reduced rate contributions' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(4)>div.panel-indent>ul.list-bullet>li:nth-child(2)", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle1.message2")
    }
  }

}
