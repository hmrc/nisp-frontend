/*
 * Copyright 2018 HM Revenue & Customs
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

import akka.util.Timeout
import ch.qos.logback.core.util.Duration
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.NispFormPartialRetriever
import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.forms.QuestionnaireForm
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.{Constants, MockTemplateRenderer}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.duration._
import akka.util.Timeout

class signedOutViewSpec  extends HtmlSpec  with MockitoSugar with BeforeAndAfter {
  implicit val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: uk.gov.hmrc.play.partials.FormPartialRetriever = NispFormPartialRetriever
  implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

  val controller = new StatePensionController {

    override val statePensionService: StatePensionService = mock[StatePensionService]
    override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]

    override lazy val customAuditConnector: CustomAuditConnector = ???
    override lazy val applicationConfig: ApplicationConfig = ???
    override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService

    override val authConnector: AuthConnector = MockAuthConnector

    override val sessionCache: SessionCache = MockSessionCache
    override val metricsService: MetricsService = MockMetricsService
    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  }

  "Signed out page" should {
    implicit val duration: Timeout = 20 seconds
    lazy val sResult = html.signedOut()
    lazy val htmlAccountDoc = asDocument(contentAsString(sResult))

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.signedOut.heading") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "display 'You've signed out of your account' header" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.signedOut.heading")
    }
    "display 'To use the service again you’ll need to sign in.' with a link" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(2)", "nisp.useagain", "/check-your-state-pension")
    }
    "display 'What did you think of this service? (takes 2 minutes).' with a link" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.signedOut.whatDidYouThink", "/check-your-state-pension/questionnaire")
    }
    "display 'Planning for retirement' header" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2", "nisp.signedOut.planningForRetirement")
    }
    "display 'Checking your State Pension is a good start...'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.signedOut.statePensionSaving")
    }
    "display 'Having more in retirement'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(6)", "nisp.signedOut.havingMoreInRetirement")
    }

    /*Side bar*/
    "render page with heading  'Pension Wise-understanding your pension options'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>h2", "nisp.questionnaire.sidebar.whatcanyoudonext")
    }
    "render page with text  'Pension Wise- understanding your pension incomes' with correct link" in {
      assertElementContainsText(htmlAccountDoc, "aside.sidebar>div>nav>ul>li:nth-child(1)", messages("nisp.questionnaire.sidebar.understandingyouroption"))
      assertElementContainsText(htmlAccountDoc, "aside.sidebar>div>nav>ul>li:nth-child(1)", "https://pensionwise.gov.uk")
    }
    "render page with text  'Planning your retirement income' with correct link" in {
      assertElementContainsText(htmlAccountDoc, "aside.sidebar>div>nav>ul>li:nth-child(2)", messages("nisp.questionnaire.sidebar.planningyourretirementincome"))
      assertElementContainsText(htmlAccountDoc, "aside.sidebar>div>nav>ul>li:nth-child(2)", "https://gov.uk/plan-retirement-income")
    }
    "render page with text  'Defer your state pension' with correct link" in {
      assertElementContainsText(htmlAccountDoc, "aside.sidebar>div>nav>ul>li:nth-child(3)", messages("nisp.questionnaire.sidebar.deferyourstatepension"))
      assertElementContainsText(htmlAccountDoc, "aside.sidebar>div>nav>ul>li:nth-child(3)", "http://gov.uk/deferring-state-pension")
    }
  }
}