/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, spy, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.nisp.utils.UnitSpec
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel, InsufficientConfidenceLevel, MissingBearerToken}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, LoginTimes, ~}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.common.RetrievalOps._

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class ExcludedAuthActionSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterEach {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]

  val nino: String = new Generator().nextNino.nino
  val fakeLoginTimes: LoginTimes = LoginTimes(Instant.now(), None)
  val credentials: Credentials = Credentials("providerId", "providerType")

  type AuthRetrievalType =
    Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes

  def makeRetrievalResults(
                            ninoOption: Option[String] = Some(nino)
                          ): Future[AuthRetrievalType] =
    Future.successful(
      ninoOption ~ ConfidenceLevel.L200 ~ Some(credentials) ~ fakeLoginTimes
    )

  override def fakeApplication(): Application  = GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[ApplicationConfig].toInstance(mockApplicationConfig)
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
    reset(mockApplicationConfig)
  }

  object Stubs {
    def successBlock(request: ExcludedAuthenticatedRequest[_]): Future[Result] = Future.successful(Ok)
  }

  val authAction: ExcludedAuthActionImpl = inject[ExcludedAuthActionImpl]

  "ExcludedAuthAction" should {
    "invoke the block" when {
      "the user details is retrieved with all the required details" in {
        when(
          mockAuthConnector
            .authorise[AuthRetrievalType](any[Predicate], any())(any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(makeRetrievalResults())

        val request = FakeRequest("", "")
        val stubs = spy(Stubs)
        val result = authAction.invokeBlock(request, stubs.successBlock)
        status(result) shouldBe OK
      }
    }
    "return error for a user without a nino" in {
      when(
        mockAuthConnector
          .authorise[AuthRetrievalType](any[Predicate], any())(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(makeRetrievalResults(ninoOption = None))

      val request = FakeRequest("", "")
      val stubs = spy(Stubs)
      val result = authAction.invokeBlock(request, stubs.successBlock)
      an[RuntimeException] should be thrownBy await(result)
    }

    "redirect the user when a type of NoActiveSession error occurs" in {
      when(
        mockAuthConnector
          .authorise[AuthRetrievalType](any[Predicate], any())(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.failed(MissingBearerToken()))

      when(mockApplicationConfig.ggSignInUrl).thenReturn("http://localhost:9949/auth-login-stub/gg-sign-in")
      when(mockApplicationConfig.postSignInRedirectUrl).thenReturn("http://localhost:9234/check-your-state-pension/account")

      val request = FakeRequest("", "")
      val stubs = spy(Stubs)
      val result = authAction.invokeBlock(request, stubs.successBlock)
      status(result) shouldBe SEE_OTHER
    }

    "redirect the user when there is an insufficient confidence level" in {
      when(
        mockAuthConnector
          .authorise[AuthRetrievalType](any[Predicate], any())(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.failed(InsufficientConfidenceLevel()))

      when(mockApplicationConfig.ivUpliftUrl).thenReturn("http://localhost:9948/iv-stub/uplift")
      when(mockApplicationConfig.postSignInRedirectUrl).thenReturn("http://localhost:9234/check-your-state-pension/account")
      when(mockApplicationConfig.notAuthorisedRedirectUrl).thenReturn("http://localhost:9234/check-your-state-pension/not-authorised")

      val request = FakeRequest("", "")
      val stubs = spy(Stubs)
      val result = authAction.invokeBlock(request, stubs.successBlock)
      status(result) shouldBe SEE_OTHER
    }
  }
}
