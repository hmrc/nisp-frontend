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



import org.joda.time.LocalDate
import play.api.i18n.Messages
import org.mockito.Mockito.when
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
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.views.html.HtmlSpec
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.play.views.formatting.Dates


class ExclusionViewSpec extends UnitSpec with MockitoSugar with HtmlSpec with BeforeAndAfter with OneAppPerSuite {


  implicit val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever

  val mockUserNino = TestAccountBuilder.regularNino;
  val mockUserIdForecastOnly =  "/auth/oid/mockforecastonly"
  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername

  lazy val fakeRequest = FakeRequest();

  val controller = new StatePensionController {

    override val statePensionService: StatePensionService = mock[StatePensionService]
    override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]

    override lazy val customAuditConnector: CustomAuditConnector = ???
    override lazy val applicationConfig: ApplicationConfig = ???
    override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService

    override protected def authConnector: AuthConnector = MockAuthConnector

    override val sessionCache: SessionCache = MockSessionCache
    override val metricsService: MetricsService = MockMetricsService
    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  }

  "Exclusion Dead" should {

    lazy val sResult = html.excluded_dead(List(Exclusion.Dead) , Some(65))
    lazy val htmlAccountDoc = asDocument(contentAsString(sResult))

    "render page with heading  You are unable to use this service " in {

      assertEqualsMessage(htmlAccountDoc ,"article.content__body>h1" , "nisp.excluded.title")
    }
    "render page with text  'Please contact HMRC National Insurance helpline on 0300 200 3500.' " in {

      assertEqualsMessage(htmlAccountDoc ,"article.content__body>p" , "nisp.excluded.dead")
    }
    "render page with help text 'Get help with this page.' " in {

      assertElementContainsText(htmlAccountDoc ,"div.report-error>a#get-help-action" , "Get help with this page.")
    }

  }
  "Exclusion Isle of Man" should {

    lazy val sResult = html.excluded_sp(List(Exclusion.IsleOfMan), Some(65), Some(new LocalDate(2028, 10, 28)), true) ;
    lazy val htmlAccountDoc = asDocument(contentAsString(sResult));

    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc ,"article.content__body>h1" , "nisp.main.h1.title")

    }
    /*"render page with heading  'You’ll reach State Pension age on' " in {
      assertContainsDynamicMessage(htmlAccountDoc ,"article.content__body>h2.heading-medium" , "(nisp.excluded.willReach, @{Dates.formatDate(new LocalDate(2028, 10, 28))})")

    }*/

   /* "render page with text  'Please contact HMRC National Insurance helpline on 0300 200 3500.' " in {
      assertElementContainsText(htmlAccountDoc ,"article.content__body>p" , Messages("nisp.excluded.isleOfMan.sp.line1"))
    }

    "render page with help text 'Get help with this page.' " in {
      assertElementContainsText(htmlAccountDoc ,"div.report-error>a#get-help-action" , Messages("nisp.excluded.isleOfMan.sp.line2"))
    }*/

  }

}
