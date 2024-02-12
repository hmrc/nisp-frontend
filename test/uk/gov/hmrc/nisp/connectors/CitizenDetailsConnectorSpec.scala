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

package uk.gov.hmrc.nisp.connectors

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, RecoverMethods}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.models.citizen.{Citizen, CitizenDetailsResponse}
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.UnitSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CitizenDetailsConnectorSpec
  extends UnitSpec
    with GuiceOneAppPerSuite
    with ScalaFutures
    with Injecting
    with BeforeAndAfterEach
    with RecoverMethods {

  implicit val headerCarrier: HeaderCarrier =
    HeaderCarrier()
  val mockMetricService: MetricsService =
    mock[MetricsService](Mockito.RETURNS_DEEP_STUBS)
  val mockApplicationConfig: ApplicationConfig =
    mock[ApplicationConfig]
  val mockHttp: HttpClient =
    mock[HttpClient]
  val nino: Nino =
    Nino("AB123456C")
  val url: String =
    "/citizen-details/AB123456C/designatory-details"
  val citizen: Citizen =
    Citizen(
      nino = nino,
      firstName = Some("John"),
      lastName = Some("Smith"),
      dateOfBirth = LocalDate.of(1983, 1, 2)
    )
  val response: CitizenDetailsResponse =
    CitizenDetailsResponse(
      person = citizen,
      address = None
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetricService)
    reset(mockApplicationConfig)
    reset(mockHttp)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[HttpClient].toInstance(mockHttp),
      bind[MetricsService].toInstance(mockMetricService)
    )
    .build()

  lazy val connector: CitizenDetailsConnector =
    inject[CitizenDetailsConnector]

  "CitizenDetailsConnector connectToGetPersonDetails" should {
    "return Right(CitizenDetailsResponse)" in {
      when(mockHttp.GET[Either[UpstreamErrorResponse, HttpResponse]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(Status.OK, Json.toJson(response), Map("" -> Seq(""))))))

      await(
        connector.connectToGetPersonDetails(nino)
      ) shouldBe Right(response)
    }

    "return Left(UpstreamErrorResponse)" in {
      when(mockHttp.GET[Either[UpstreamErrorResponse, HttpResponse]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("This is the error", 502, 502))))

      await(
        connector.connectToGetPersonDetails(nino)
      ).swap.getOrElse(UpstreamErrorResponse) shouldBe a[UpstreamErrorResponse]
    }

    "return Left(Throwable)" in {
      when(mockHttp.GET[Either[UpstreamErrorResponse, HttpResponse]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("This is the error", 500, 500))))

      await(
        connector.connectToGetPersonDetails(nino)
      ).swap.getOrElse(UpstreamErrorResponse) shouldBe a[Throwable]
    }

    "return a Throwable for failed request" in {
      when(mockHttp.GET[Either[UpstreamErrorResponse, HttpResponse]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new Throwable("This is a Throwable")))

      await(
        connector.connectToGetPersonDetails(nino).failed
      ) shouldBe a[Throwable]
    }

    "return a Left(UpstreamErrorResponse) for failed request HttpException" in {
      when(mockHttp.GET[Either[UpstreamErrorResponse, HttpResponse]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new HttpException("This is an error", 503)))

      recoverToSucceededIf[HttpException] {
        connector.connectToGetPersonDetails(nino).swap.getOrElse(UpstreamErrorResponse) shouldBe a[UpstreamErrorResponse]
      }
    }
  }
}
