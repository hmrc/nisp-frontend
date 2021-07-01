/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.Mockito.{RETURNS_DEEP_STUBS, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.nisp.helpers.FakeSessionCache
import uk.gov.hmrc.nisp.models.NationalInsuranceRecord
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.JsonDepersonaliser
import uk.gov.hmrc.play.test.UnitSpec
import play.api.http.Status.OK
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class BackendConnectorSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val mockHttp: HttpClient = mock[HttpClient]
  val mockMetricsService = mock[MetricsService](RETURNS_DEEP_STUBS)

  object BackendConnectorImpl extends BackendConnector {
    override def http: HttpClient = mockHttp
    override def sessionCache: SessionCache = FakeSessionCache
    override def serviceUrl: String = "national-insurance"
    override val metricsService: MetricsService = mockMetricsService
    override implicit val executionContext: ExecutionContext = global

    def getNationalInsurance()(implicit headerCarrier: HeaderCarrier): Future[NationalInsuranceRecord] = {
      val urlToRead = s"$serviceUrl/ni"
      retrieveFromCache[NationalInsuranceRecord](APIType.NationalInsurance, urlToRead)(headerCarrier, NationalInsuranceRecord.formats)
    }
  }

  implicit val headerCarrier = HeaderCarrier()

  "connectToMicroservice" should {
    "should return depersonalised JSON" in {
      val json = Json.obj(
        "qualifyingYearsPriorTo1975" -> 0,
        "numberOfGaps" -> 6,
        "numberOfGapsPayable" -> 4,
        "dateOfEntry" -> "1975-08-01",
        "homeResponsibilitiesProtection" -> false,
        "earningsIncludedUpTo" -> "2016-04-05",
        "_embedded" -> Json.obj(
          "taxYears" -> Json.arr()
        )
      )

      val depersonalisedJson =  JsonDepersonaliser.depersonalise(json) match {
        case Success(s) => s
        case Failure(_) => fail()
      }

      val response = Future(HttpResponse(OK, json, Map.empty[String, Seq[String]]))
      when(mockHttp.GET[HttpResponse]("national-insurance/ni", Seq(), Seq())).thenReturn(response)

      val future: Future[NationalInsuranceRecord] = BackendConnectorImpl.getNationalInsurance()

      whenReady(future.failed) {
        t: Throwable =>
          t.getMessage.contains(depersonalisedJson) shouldBe true
          t.getMessage.contains("2016-04-05") shouldBe false
      }
    }
  }

}
