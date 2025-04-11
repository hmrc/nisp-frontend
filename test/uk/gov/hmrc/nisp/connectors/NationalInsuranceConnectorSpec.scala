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
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers, Mockito}
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, RecoverMethods}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.{NationalInsuranceRecord, StatePensionExclusion}
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.UnitSpec

import java.time.LocalDate
import scala.concurrent.Future

class NationalInsuranceConnectorSpec extends UnitSpec
  with GuiceOneAppPerSuite
  with ScalaFutures
  with Injecting
  with BeforeAndAfterEach
  with RecoverMethods {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  val mockMetricService: MetricsService = mock[MetricsService](Mockito.RETURNS_DEEP_STUBS)
  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockHttp: HttpClientV2 = mock[HttpClientV2](Mockito.RETURNS_DEEP_STUBS)
  val nino: Nino = Nino("AB123456C")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApplicationConfig)
    reset(mockMetricService)
    reset(mockHttp)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[HttpClientV2].toInstance(mockHttp),
      bind[MetricsService].toInstance(mockMetricService)
    )
    .build()

  lazy val connector: NationalInsuranceConnector =
    inject[NationalInsuranceConnector]

  "NationalInsuranceConnector getNationalInsurance" should {

    "return NationalInsuranceRecord" in {

      val nir = NationalInsuranceRecord(
        qualifyingYears = 1,
        qualifyingYearsPriorTo1975 = 2,
        numberOfGaps = 3,
        numberOfGapsPayable = 4,
        Some(LocalDate.of(1,2,3)),
        homeResponsibilitiesProtection = true,
        LocalDate.of(1,2,3),
        List.empty,
        reducedRateElection = false)

      val headerCaptor: ArgumentCaptor[(String,String)] = ArgumentCaptor.forClass(classOf[(String,String)])

      when(mockHttp.get(ArgumentMatchers.eq(url"http://localhost:9312/ni/mdtp/AB123456C"))(any())
        .setHeader(headerCaptor.capture())
        .execute[Either[UpstreamErrorResponse, Either[StatePensionExclusion, NationalInsuranceRecord]]](using any(), any()))
        .thenReturn(Future.successful(Right(Right(nir))))

      await(
        connector.getNationalInsurance(nino)
      ) shouldBe Right(Right(nir))

      eventually {
        headerCaptor.getAllValues.toArray.mkString shouldBe "List((Accept,application/vnd.hmrc.1.0+json))"
        verify(mockMetricService.startTimer(ArgumentMatchers.eq(APIType.NationalInsurance)), times(1)).stop()
      }
    }

    "return an error" in {
      val errorResponse = new IllegalArgumentException("test")
      when(mockHttp.get(ArgumentMatchers.eq(url"http://localhost:9312/ni/mdtp/AB123456C"))(any())
        .setHeader(any())
        .execute[Either[UpstreamErrorResponse, Either[StatePensionExclusion, NationalInsuranceRecord]]](using any(), any()))
        .thenReturn(Future.failed(errorResponse))

      whenReady(connector.getNationalInsurance(nino).failed) { e =>
        e shouldBe errorResponse
      }
    }
  }
}
