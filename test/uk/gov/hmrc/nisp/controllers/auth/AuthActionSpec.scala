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
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, Controller}
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, LoginTimes, Retrieval, ~}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments, SessionRecordNotFound}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.nisp.common.RetrievalOps._
import uk.gov.hmrc.nisp.config.wiring.NispAuthConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.citizen.{NOT_FOUND, TECHNICAL_DIFFICULTIES}
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AuthActionSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  class BrokenAuthConnector(exception: Throwable) extends NispAuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exception)
  }

  private val mockAuthConnector: NispAuthConnector = mock[NispAuthConnector]

  private val nino: String = new Generator().nextNino.nino
  private val fakeLoginTimes = LoginTimes(DateTime.now(), None)

  class Harness(authAction: AuthAction) extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction { request => Ok }
  }

  val ggSignInUrl = "http://localhost:9949/auth-login-stub/gg-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&origin=nisp-frontend&accountType=individual"
  implicit val timeout: Timeout = 5 seconds

  "GET /statepension" should {
    "return 303 when no session" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new SessionRecordNotFound))
      val cds: CitizenDetailsService = new CitizenDetailsService(MockCitizenDetailsConnector)
      val authAction = new AuthActionImpl(mockAuthConnector, cds)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should endWith(ggSignInUrl)
    }

    "return error for not found user" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      val retrievalResults: Future[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments] =
        Future.successful(Some(nino) ~ ConfidenceLevel.L200 ~ None ~ fakeLoginTimes ~ Enrolments(Set.empty))

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(retrievalResults)

      when(mockCitizenDetailsService.retrievePerson(any())(any()))
        .thenReturn(Future.successful(Left(NOT_FOUND)))

      val authAction: AuthActionImpl = new AuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      an[InternalServerException] should be thrownBy await(result)
    }

    "return error for technical difficulties" in {
      val mockCitizenDetailsService = mock[CitizenDetailsService]

      val retrievalResults: Future[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments] =
        Future.successful(Some(nino) ~ ConfidenceLevel.L200 ~ None ~ fakeLoginTimes ~ Enrolments(Set.empty))

      when(mockAuthConnector.authorise[Option[String] ~ ConfidenceLevel ~ Option[Credentials] ~ LoginTimes ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(retrievalResults)

      when(mockCitizenDetailsService.retrievePerson(any())(any()))
        .thenReturn(Future.successful(Left(TECHNICAL_DIFFICULTIES)))

      val authAction: AuthActionImpl = new AuthActionImpl(mockAuthConnector, mockCitizenDetailsService)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      an[InternalServerException] should be thrownBy await(result)
    }

  }

}
