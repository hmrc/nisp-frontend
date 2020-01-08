/*
 * Copyright 2020 HM Revenue & Customs
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
import org.joda.time.{DateTime, LocalDate}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{spy, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.nisp.common.RetrievalOps._
import uk.gov.hmrc.nisp.config.wiring.NispAuthConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.UserName
import uk.gov.hmrc.nisp.models.citizen._
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class VerifyAuthActionSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  class BrokenAuthConnector(exception: Throwable) extends NispAuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exception)
  }

  private val mockAuthConnector: NispAuthConnector = mock[NispAuthConnector]

  private val nino: String = new Generator().nextNino.nino
  private val fakeLoginTimes = LoginTimes(DateTime.now(), None)
  private val credentials = Credentials("providerId", "providerType")

  private val citizen: Citizen = Citizen(Nino(nino), Some("John"), Some("Smith"), new LocalDate(1983, 1, 2))
  private val address: Address = Address(Some("Country"))
  private val citizenDetailsResponse: CitizenDetailsResponse = CitizenDetailsResponse(citizen, Some(address))

  private def makeRetrievalResults(ninoOption: Option[String] = Some(nino), enrolments: Enrolments = Enrolments(Set.empty)): Future[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments] =
    Future.successful(ninoOption ~ ConfidenceLevel.L500 ~ Some(credentials) ~ fakeLoginTimes ~ enrolments)

  private object Stubs {
    def successBlock(request: AuthenticatedRequest[_]): Future[Result] = Future.successful(Ok)
  }

  val verifyUrl = "http://localhost:9949/auth-login-stub/verify-sign-in"
  val upliftUrlTail = "/uplift?origin=NISP&completionURL=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&failureURL=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Fnot-authorised&confidenceLevel=200"
  implicit val timeout: Timeout = 5 seconds

  "GET /signin/verify" should {
    "invoke the block when the user details can be retrieved without SA enrolment" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(makeRetrievalResults())

      when(mockCitizenDetailsService.retrievePerson(any())(any()))
        .thenReturn(Future.successful(Right(citizenDetailsResponse)))

      val stubs = spy(Stubs)

      val authAction: VerifyAuthActionImpl = new VerifyAuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val request = FakeRequest("", "")
      val result = authAction.invokeBlock(request, stubs.successBlock)
      status(result) shouldBe OK

      val expectedAuthenticatedRequest = AuthenticatedRequest(request,
        NispAuthedUser(Nino(nino),
          citizen.dateOfBirth,
          UserName(Name(citizen.firstName, citizen.lastName)),
          citizenDetailsResponse.address,
          isSa = false),
        AuthDetails(ConfidenceLevel.L500, Some("providerType"), fakeLoginTimes))
      verify(stubs).successBlock(expectedAuthenticatedRequest)
    }

    "invoke the block when the user details can be retrieved withSA enrolment" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(makeRetrievalResults(enrolments = Enrolments(Set(Enrolment("IR-SA")))))

      when(mockCitizenDetailsService.retrievePerson(any())(any()))
        .thenReturn(Future.successful(Right(citizenDetailsResponse)))

      val stubs = spy(Stubs)

      val authAction: VerifyAuthActionImpl = new VerifyAuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val request = FakeRequest("", "")
      val result = authAction.invokeBlock(request, stubs.successBlock)
      status(result) shouldBe OK

      val expectedAuthenticatedRequest = AuthenticatedRequest(request,
        NispAuthedUser(Nino(nino),
          citizen.dateOfBirth,
          UserName(Name(citizen.firstName, citizen.lastName)),
          citizenDetailsResponse.address,
          isSa = true),
        AuthDetails(ConfidenceLevel.L500, Some("providerType"), fakeLoginTimes))
      verify(stubs).successBlock(expectedAuthenticatedRequest)
    }

    "redirect to Verify when no session" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound()))
      val cds: CitizenDetailsService = new CitizenDetailsService(MockCitizenDetailsConnector)
      val authAction: VerifyAuthActionImpl = new VerifyAuthActionImpl(mockAuthConnector, cds)
      val result = authAction.invokeBlock(FakeRequest("", ""), Stubs.successBlock)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should startWith(verifyUrl)
    }

    "redirect to Verify when insufficient confidence level" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientConfidenceLevel()))
      val cds: CitizenDetailsService = new CitizenDetailsService(MockCitizenDetailsConnector)
      val authAction: VerifyAuthActionImpl = new VerifyAuthActionImpl(mockAuthConnector, cds)
      val result = authAction.invokeBlock(FakeRequest("", ""), Stubs.successBlock)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should startWith(verifyUrl)
    }

    "redirect to Verify sign in when auth provider is not Verify" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(UnsupportedAuthProvider()))
      val cds: CitizenDetailsService = new CitizenDetailsService(MockCitizenDetailsConnector)
      val authAction: VerifyAuthActionImpl = new VerifyAuthActionImpl(mockAuthConnector, cds)
      val result = authAction.invokeBlock(FakeRequest("", ""), Stubs.successBlock)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should startWith(verifyUrl)
    }

    "return error for not found user" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(makeRetrievalResults())

      when(mockCitizenDetailsService.retrievePerson(any())(any()))
        .thenReturn(Future.successful(Left(NOT_FOUND)))

      val authAction: VerifyAuthActionImpl = new VerifyAuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val result = authAction.invokeBlock(FakeRequest("", ""), Stubs.successBlock)
      an[InternalServerException] should be thrownBy await(result)
    }

    "return error for user without nino" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(makeRetrievalResults(ninoOption = None))

      val authAction: VerifyAuthActionImpl = new VerifyAuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val result = authAction.invokeBlock(FakeRequest("", ""), Stubs.successBlock)
      an[RuntimeException] should be thrownBy await(result)
    }

    "return error for technical difficulties" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(makeRetrievalResults())

      when(mockCitizenDetailsService.retrievePerson(any())(any()))
        .thenReturn(Future.successful(Left(TECHNICAL_DIFFICULTIES)))

      val authAction: VerifyAuthActionImpl = new VerifyAuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val result = authAction.invokeBlock(FakeRequest("", ""), Stubs.successBlock)
      an[InternalServerException] should be thrownBy await(result)
    }

    "return redirect for exclusion when NI" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(makeRetrievalResults())

      when(mockCitizenDetailsService.retrievePerson(any())(any()))
        .thenReturn(Future.successful(Left(MCI_EXCLUSION)))

      val authAction: VerifyAuthActionImpl = new VerifyAuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val result = authAction.invokeBlock(FakeRequest("", "a-uri-with-nirecord"), Stubs.successBlock)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusionni")
    }
  }

  "return redirect for exclusion when not NI" in {
    val mockCitizenDetailsService = mock[CitizenDetailsService]

    when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
      .thenReturn(makeRetrievalResults())

    when(mockCitizenDetailsService.retrievePerson(any())(any()))
      .thenReturn(Future.successful(Left(MCI_EXCLUSION)))

    val authAction: VerifyAuthActionImpl = new VerifyAuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
    val result = authAction.invokeBlock(FakeRequest("", "a-non-ni-record-uri"), Stubs.successBlock)
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusion")
  }
}
