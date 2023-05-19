/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.errorHandler

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.models.admin.{ExcessiveTrafficToggle, FeatureFlag}
import uk.gov.hmrc.nisp.services.admin.FeatureFlagService
import uk.gov.hmrc.nisp.utils.UnitSpec

import java.util.Locale
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ErrorHandlerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterEach {

  implicit val request: Request[_] = FakeRequest()

  lazy val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]
  lazy val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[ApplicationConfig].toInstance(mockApplicationConfig),
      bind[FeatureFlagService].toInstance(mockFeatureFlagService)
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockApplicationConfig.urBannerUrl).thenReturn("/urResearch")
    when(mockApplicationConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
    when(mockApplicationConfig.contactFormServiceIdentifier).thenReturn("/id")
    when(mockApplicationConfig.showExcessiveTrafficMessage).thenReturn(false)
  }

  lazy val errorHandler: ErrorHandler = inject[ErrorHandler]
  implicit val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  "standardErrorTemplate" must {
    "return the global error view" in {
      val title   = "testTitle"
      val heading = "testHeading"
      val message = "testMessage"

      val standardErrorTemplate: Html = errorHandler.standardErrorTemplate(title, heading, message)
      val doc: Document               = Jsoup.parse(standardErrorTemplate.toString())

      val docTitle   = doc.select("title").text()
      val docHeading = doc.select("h1").text()
      val docMessage = doc.select("p").text()

      docTitle   shouldBe title
      docHeading shouldBe heading
      docMessage shouldBe message
    }
  }

  "internalServerErrorTemplate" must {
    "return excessiveTrafficMessage when showExcessiveTrafficMessage is true" in {
      val returnedFeatureFlag = FeatureFlag(ExcessiveTrafficToggle, isEnabled = true)
      when(mockFeatureFlagService.get(ExcessiveTrafficToggle)).thenReturn(Future.successful(returnedFeatureFlag))

      val serverErrorTemplate: Future[String] = errorHandler.internalServerErrorTemplate.map(_.toString)
      val doc: Document = await(serverErrorTemplate.map(Jsoup.parse))

      val docTitle = doc.select("title").text()
      docTitle should include(messages("global.error.InternalServerError500.title"))

    }

    "return 'try again later' when showExcessiveTrafficMessage is false" in {
      val returnedFeatureFlag = FeatureFlag(ExcessiveTrafficToggle, isEnabled = false)
      when(mockFeatureFlagService.get(ExcessiveTrafficToggle)).thenReturn(Future.successful(returnedFeatureFlag))
      val serverErrorTemplate: Future[String] = errorHandler.internalServerErrorTemplate.map(_.toString)
      val doc: Document = await(serverErrorTemplate.map(Jsoup.parse))

      val docTitle = doc.select("title").text()
      docTitle should include(messages("global.error.InternalServerError500.title"))

    }
  }

  "notFoundTemplate" must {
    "return the page not found view" in {
      val notFoundTemplate: Html = errorHandler.notFoundTemplate
      val doc: Document          = Jsoup.parse(notFoundTemplate.toString())

      val docTitle = doc.select("title").text()
      docTitle should include(messages("global.page.not.found.error.title"))
    }
  }
}
