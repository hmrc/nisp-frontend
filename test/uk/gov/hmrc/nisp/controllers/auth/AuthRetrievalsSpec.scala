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

import org.apache.pekko.util.Timeout
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{reset, spy, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.Helpers.redirectLocation
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.nisp.common.RetrievalOps._
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.models.UserName
import uk.gov.hmrc.nisp.models.citizen._
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.nisp.utils.{EqualsAuthenticatedRequest, UnitSpec}

import java.time.{Instant, LocalDate}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AuthRetrievalsSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterEach {

  class BrokenAuthConnector(exception: Throwable) extends AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
    ): Future[A] =
      Future.failed(exception)
  }

  type AuthRetrievalType =
    Option[String] ~ ConfidenceLevel ~ Option[String] ~ Option[Credentials] ~ LoginTimes ~ Enrolments ~ Option[TrustedHelper]

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockCitizenDetailsService: CitizenDetailsService = mock[CitizenDetailsService]

  val nino: String = new Generator().nextNino.nino
  val fakeLoginTimes: LoginTimes = LoginTimes(Instant.now(), None)
  val credentials: Credentials = Credentials("providerId", "providerType")
  val citizen: Citizen = Citizen(Nino(nino), Some("John"), Some("Smith"), LocalDate.of(1983, 1, 2))
  val address: Address = Address(Some("Country"))
  val citizenDetailsResponse: CitizenDetailsResponse = CitizenDetailsResponse(citizen, Some(address))

  when(mockApplicationConfig.gracePeriodStartDay).thenReturn(5)
  when(mockApplicationConfig.gracePeriodStartMonth).thenReturn(4)
  when(mockApplicationConfig.gracePeriodEndDay).thenReturn(7)
  when(mockApplicationConfig.gracePeriodEndMonth).thenReturn(5)

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[ApplicationConfig].toInstance(mockApplicationConfig),
      bind[CitizenDetailsService].toInstance(mockCitizenDetailsService)
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
    reset(mockApplicationConfig)
    reset(mockCitizenDetailsService)
  }

  def makeRetrievalResults(
    ninoOption: Option[String] = Some(nino),
    enrolments: Enrolments = Enrolments(Set.empty),
    credentialStrength: String = CredentialStrength.strong,
    trustedHelper: Option[TrustedHelper] = None
  ): Future[AuthRetrievalType] =
    Future.successful(
      ninoOption ~ ConfidenceLevel.L200 ~ Some(credentialStrength) ~ Some(credentials) ~ fakeLoginTimes ~ enrolments ~ trustedHelper
    )

  class Stubs {
    def successBlock(request: AuthenticatedRequest[?]): Future[Result] = Future.successful(Ok)
  }

  val ggSignInUrlTail           =
    "/auth-login-stub/gg-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&origin=nisp-frontend&accountType=individual"
  val upliftUrlTail             =
    "/mdtp/uplift?origin=NISP&completionURL=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&failureURL=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Fnot-authorised&confidenceLevel=200"
  implicit val timeout: Timeout = 5 seconds

  val authAction: AuthRetrievalsImpl = inject[AuthRetrievalsImpl]

  "GET /statepension" should {
    "invoke the block" when {
      "the user details can be retrieved without SA enrolment" in {
        when(
          mockAuthConnector
            .authorise[AuthRetrievalType](any[Predicate], any())(any[HeaderCarrier], any[ExecutionContext])
        )
          .thenReturn(makeRetrievalResults())

        when(mockCitizenDetailsService.retrievePerson(any[Nino])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(citizenDetailsResponse)))

        val stubs   = spy(new Stubs)
        val request = FakeRequest("", "")
        val result  = authAction.invokeBlock(request, stubs.successBlock)
        status(result) shouldBe OK

        val expectedAuthenticatedRequest = AuthenticatedRequest(
          request,
          NispAuthedUser(
            Nino(nino),
            citizen.dateOfBirth,
            UserName(Name(citizen.firstName, citizen.lastName)),
            citizenDetailsResponse.address,
            None,
            isSa = false
          ),
          AuthDetails(ConfidenceLevel.L200, fakeLoginTimes)
        )
        verify(stubs).successBlock(argThat(EqualsAuthenticatedRequest(expectedAuthenticatedRequest)))
      }

      "the user details can be retrieved withSA enrolment" in {
        when(
          mockAuthConnector
            .authorise[AuthRetrievalType](any[Predicate], any())(any[HeaderCarrier], any[ExecutionContext])
        )
          .thenReturn(makeRetrievalResults(enrolments = Enrolments(Set(Enrolment("IR-SA")))))

        when(mockCitizenDetailsService.retrievePerson(any[Nino])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(citizenDetailsResponse)))

        val stubs   = spy(new Stubs)
        val request = FakeRequest("", "")
        val result  = authAction.invokeBlock(request, stubs.successBlock)
        status(result) shouldBe OK

        val expectedAuthenticatedRequest = AuthenticatedRequest(
          request,
          NispAuthedUser(
            Nino(nino),
            citizen.dateOfBirth,
            UserName(Name(citizen.firstName, citizen.lastName)),
            citizenDetailsResponse.address,
            None,
            isSa = true
          ),
          AuthDetails(ConfidenceLevel.L200, fakeLoginTimes)
        )
        verify(stubs).successBlock(argThat(EqualsAuthenticatedRequest(expectedAuthenticatedRequest)))
      }

      "the user details is a trusted helper" in {
        val trustedHelperNino = new Generator().nextNino.nino
        val trustedHelper     = TrustedHelper("pName", "aName", "link", Some(trustedHelperNino))

        when(mockAuthConnector.authorise[AuthRetrievalType](any(), any())(any(), any()))
          .thenReturn(makeRetrievalResults(trustedHelper = Some(trustedHelper)))

        when(mockCitizenDetailsService.retrievePerson(any())(any()))
          .thenReturn(Future.successful(Right(citizenDetailsResponse)))

        val stubs   = spy(new Stubs)
        val request = FakeRequest("", "")
        val result  = authAction.invokeBlock(request, stubs.successBlock)
        status(result) shouldBe OK

        val expectedAuthenticatedRequest = AuthenticatedRequest(
          request,
          NispAuthedUser(
            Nino(trustedHelperNino),
            citizen.dateOfBirth,
            UserName(Name(citizen.firstName, citizen.lastName)),
            citizenDetailsResponse.address,
            Some(trustedHelper),
            isSa = false
          ),
          AuthDetails(ConfidenceLevel.L200, fakeLoginTimes)
        )
        verify(stubs).successBlock(argThat(EqualsAuthenticatedRequest(expectedAuthenticatedRequest)))
      }
    }

    "return error for not found user" in {
      when(
        mockAuthConnector.authorise[AuthRetrievalType](any[Predicate], any())(any[HeaderCarrier], any[ExecutionContext])
      )
        .thenReturn(makeRetrievalResults())

      when(mockCitizenDetailsService.retrievePerson(any[Nino])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(NOT_FOUND)))

      val stubs = new Stubs

      val result = authAction.invokeBlock(FakeRequest("", ""), stubs.successBlock)
      an[InternalServerException] should be thrownBy await(result)
    }

    "return error for user without nino" in {
      when(
        mockAuthConnector.authorise[AuthRetrievalType](any[Predicate], any())(any[HeaderCarrier], any[ExecutionContext])
      )
        .thenReturn(makeRetrievalResults(ninoOption = None))

      val stubs = new Stubs

      val result = authAction.invokeBlock(FakeRequest("", ""), stubs.successBlock)
      an[RuntimeException] should be thrownBy await(result)
    }

    "return error for technical difficulties" in {
      when(
        mockAuthConnector.authorise[AuthRetrievalType](any[Predicate], any())(any[HeaderCarrier], any[ExecutionContext])
      )
        .thenReturn(makeRetrievalResults())

      when(mockCitizenDetailsService.retrievePerson(any[Nino])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(TECHNICAL_DIFFICULTIES)))

      val stubs = new Stubs

      val result = authAction.invokeBlock(FakeRequest("", ""), stubs.successBlock)
      an[InternalServerException] should be thrownBy await(result)
    }

    "return redirect for exclusion when NI" in {
      when(
        mockAuthConnector.authorise[AuthRetrievalType](any[Predicate], any())(any[HeaderCarrier], any[ExecutionContext])
      )
        .thenReturn(makeRetrievalResults())

      when(mockCitizenDetailsService.retrievePerson(any[Nino])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(MCI_EXCLUSION)))

      val stubs = new Stubs

      val result = authAction.invokeBlock(FakeRequest("", "a-uri-with-nirecord"), stubs.successBlock)
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusionni")
    }

    "return redirect for exclusion when not NI" in {
      when(
        mockAuthConnector.authorise[AuthRetrievalType](any[Predicate], any())(any[HeaderCarrier], any[ExecutionContext])
      )
        .thenReturn(makeRetrievalResults())

      when(mockCitizenDetailsService.retrievePerson(any[Nino])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(MCI_EXCLUSION)))

      val stubs = new Stubs

      val result = authAction.invokeBlock(FakeRequest("", "a-non-ni-record-uri"), stubs.successBlock)
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusion")
    }
  }
}
