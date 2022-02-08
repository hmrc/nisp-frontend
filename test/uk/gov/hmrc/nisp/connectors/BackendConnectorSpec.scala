/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{RETURNS_DEEP_STUBS, when}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.helpers.FakeSessionCache
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.{NationalInsuranceRecord, StatePensionExclusion}
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class BackendConnectorSpec extends UnitSpec with ScalaFutures {

  val mockHttp: HttpClient = mock[HttpClient]
  val mockMetricsService   = mock[MetricsService](RETURNS_DEEP_STUBS)

  object BackendConnectorImpl extends BackendConnector {
    override def http: HttpClient                            = mockHttp
    override def sessionCache: SessionCache                  = FakeSessionCache
    override def serviceUrl: String                          = "national-insurance"
    override val metricsService: MetricsService              = mockMetricsService
    override implicit val executionContext: ExecutionContext = global
    implicit val hc: HeaderCarrier = HeaderCarrier()

    def getNationalInsurance()(implicit headerCarrier: HeaderCarrier): Future[Either[UpstreamErrorResponse, Either[StatePensionExclusion, NationalInsuranceRecord]]] = {
      val urlToRead = s"$serviceUrl/ni"
      retrieveFromCache(APIType.NationalInsurance, urlToRead)(headerCarrier, NationalInsuranceRecord.formats)
    }
  }

  implicit val headerCarrier = HeaderCarrier()

  "connectToMicroservice" should {
    "should return UpstreamErrorResponse and failed future" in {
      val json = Json.obj(
        "qualifyingYearsPriorTo1975"     -> 0,
        "numberOfGaps"                   -> 6,
        "numberOfGapsPayable"            -> 4,
        "dateOfEntry"                    -> "1975-08-01",
        "homeResponsibilitiesProtection" -> false,
        "earningsIncludedUpTo"           -> "2016-04-05",
        "_embedded"                      -> Json.obj(
          "taxYears" -> Json.arr()
        )
      )

      val response = Future(HttpResponse(OK, json, Map.empty[String, Seq[String]]))
      when(
        mockHttp.GET[HttpResponse](
          ArgumentMatchers.eq("national-insurance/ni"),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      )
        .thenReturn(response)

      val future: Future[Either[UpstreamErrorResponse, Either[StatePensionExclusion, NationalInsuranceRecord]]] =
        BackendConnectorImpl.getNationalInsurance()

      whenReady(future.failed) {
        t: Throwable =>
          t.getMessage.contains("2016-04-05") shouldBe false
      }
    }
  }

}
