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

import java.util.UUID

import org.joda.time.LocalDate
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.{NationalInsuranceRecord, NationalInsuranceTaxYear}
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService}
import uk.gov.hmrc.nisp.views.html.HtmlSpec
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils.now






class NIRecordViewSpec extends UnitSpec with MockitoSugar with HtmlSpec with BeforeAndAfter with OneAppPerSuite {



  implicit val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever

  val mockUserNino = TestAccountBuilder.regularNino;
  val mockUserIdForecastOnly =  "/auth/oid/mockforecastonly"
  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername
  val mockFullUserId = "/auth/oid/mockfulluser"

  lazy val fakeRequest = FakeRequest();




  def authenticatedFakeRequest(userId: String) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId,
    SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
  )




  "Render Ni Record view with Gaps Only" should {

    lazy val controller = new MockNIRecordController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
      override val sessionCache: SessionCache = MockSessionCache
      override val showFullNI = true
      override val currentDate = new LocalDate(2016, 9, 9)

      override protected def authConnector: AuthConnector = MockAuthConnector

      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val metricsService: MetricsService = MockMetricsService
    }

    lazy val result = controller.showGaps(authenticatedFakeRequest(mockUserId))

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    /*Check side border :summary */
    "render page with heading  Summary" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>h2", "nisp.nirecord.summary.yourrecord")
    }
    "render page with number of qualifying yeras - 28" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(2)", "28")
    }
    "render page with text 'years of full contributions'" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(3)", "nisp.nirecord.summary.fullContributions")
    }
    "render page with 4 years to contribute before 5 April 2018 '" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(4)", "4")
    }
    "render page with text  'years to contribute before 5 April 2017'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(5)", "nisp.nirecord.summary.yearsRemaining" , "2018" ,null ,null)
    }
    "render page with 10 years - when you did not contribute enough" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(6)", "10")
    }
    "render page with text 'when you did not contribute enough'" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(7)", "nisp.nirecord.summary.gaps")
    }
   /*Ends here*/
    "render page with Gaps  heading  Your National Insurance record " in {
        assertEqualsMessage(htmlAccountDoc, "h1.heading-large", "nisp.nirecord.heading")
      }
    "render page with text 'Years which are not full'" in {
      assertEqualsMessage(htmlAccountDoc, "p.lede", "nisp.nirecord.yournirecordgapyears")
    }
    "render page with link 'View all years'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)>a", "nisp.nirecord.showfull")
    }

    "render page with link href 'View all years'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(4)>a", "/check-your-state-pension/account/nirecord")
    }

    "render page with text  'year is not full'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(2)>div>div.ni-notfull", "nisp.nirecord.gap")
    }
    "render page with link 'View details'" in {

      assertContainsTextBetweenTags(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(2)>div>div.ni-action>a", "View  details" ,"article.content__body>dl:nth-child(5)>dt:nth-child(2)>div>div.ni-action>a>span")
    }

    "render page with text  'You did not make any contributions this year '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header", "nisp.nirecord.youdidnotmakeanycontrib")
    }

    "render page with text 'Find out more about'" in {
      assertContainsTextBetweenTags(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)", "Find out more about ." ,"article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a")
    }

    "render page with link 'gaps in your record and how to check them'" in {
      assertEqualsValue(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a", "gaps in your record and how to check them")
    }
    "render page with link href 'gaps in your record and how to check them'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a", "/check-your-state-pension/account/nirecord/gapsandhowtocheck")
    }

    "render page with text  'You can make up the shortfall'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header:nth-child(3)", "nisp.nirecord.gap.youcanmakeupshortfall")
    }
    "render page with text  'Pay a voluntary contribution of figure out how to do it...'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(4)", "nisp.nirecord.gap.payvoluntarycontrib" , " &pound;704.60","5 April 2023" ,"5 April 2019" )
    }
    "render page with text  'Find out more about...'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(5)","nisp.nirecord.gap.findoutmore" ,"/check-your-state-pension/account/nirecord/voluntarycontribs" ,null,null)
    }
    "render page with text  ' year is not full'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(10)>div>div.ni-notfull", "nisp.nirecord.gap")
    }

    "render page with text  'You did not make any contributions this year for toolate to pay '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p.contributions-header", "nisp.nirecord.youdidnotmakeanycontrib")
    }

    "render page with text 'Find out more about for toolate to pay'" in {
      assertContainsTextBetweenTags(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p:nth-child(2)", "Find out more about ." ,"article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a")
    }

    "render page with link 'gaps in your record and how to check them for toolate to pay'" in {
      assertEqualsValue(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p:nth-child(2)>a", "gaps in your record and how to check them")
    }
    "render page with link href 'gaps in your record and how to check them for toolate to pay'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p:nth-child(2)>a", "/check-your-state-pension/account/nirecord/gapsandhowtocheck")
    }

    "render page with text  'It’s too late to pay for this year. You can usually only pay for the last 6 years.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p.panel-indent:nth-child(3)", "nisp.nirecord.gap.latePaymentMessage")
    }
    "render page with link  'view all years'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p>a", "nisp.nirecord.showfull")
    }
    "render page with href link  'view all years'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p>a", "/check-your-state-pension/account/nirecord")
    }
    "render page with link  'back'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.backlink>a", "nisp.back")
    }
    "render page with href link  'back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.backlink>a", "/check-your-state-pension/account")
    }


  }


}
