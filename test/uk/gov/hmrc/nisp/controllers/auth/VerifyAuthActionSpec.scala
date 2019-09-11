/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.controllers.auth

import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, Controller}
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken, SessionRecordNotFound}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.CitizenDetailsConnector
import uk.gov.hmrc.nisp.helpers.{MockAuthConnector, MockCachedStaticHtmlPartialRetriever, MockCitizenDetailsConnector, MockCitizenDetailsService, MockStatePensionController}
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class VerifyAuthActionSpec extends UnitSpec with OneAppPerSuite with ScalaFutures with MockitoSugar {

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[NispAuthConnector].toInstance(mockAuthConnector))
    .overrides(bind[CitizenDetailsService].toInstance(MockCitizenDetailsService))
    .overrides(bind[CitizenDetailsConnector].toInstance(MockCitizenDetailsConnector))
    .build()

  val verifyUrl = "http://localhost:9949/auth-login-stub/verify-sign-in"
  implicit val timeout: Timeout = 5 seconds

  class BrokenAuthConnector(exception: Throwable) extends NispAuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exception)
  }

  class Harness(authAction: AuthAction) extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction { request => Ok }
  }

  val mockAuthConnector: NispAuthConnector = mock[NispAuthConnector]

  "GET /signin/verify" should {
    "return 303 and redirect to verify when No Session" in {
      val cds: CitizenDetailsService = new CitizenDetailsService(MockCitizenDetailsConnector)
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("")))
      val authAction = new VerifyAuthActionImpl(mockAuthConnector, cds)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should startWith(verifyUrl)
    }

    "return error for blank user" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new InternalServerException("")))
      val authAction: VerifyAuthActionImpl = new VerifyAuthActionImpl(mockAuthConnector, MockCitizenDetailsService)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      an[InternalServerException] should be thrownBy await(result)
    }

    "redirect to Verify with IV disabled" in {
      val applicationConfig: ApplicationConfig = mock[ApplicationConfig]
      when(applicationConfig.identityVerification).thenReturn(false)
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(MissingBearerToken("Missing Bearer Token!")))
      val authAction = AuthActionSelector.decide(applicationConfig)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:9949/auth-login-stub/verify-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount")
    }

  }

}
