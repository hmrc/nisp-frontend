/*
 * Copyright 2017 HM Revenue & Customs
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

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.helpers.{MockMetricsService, MockSessionCache}
import uk.gov.hmrc.nisp.models.NationalInsuranceRecord
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.JsonDepersonaliser
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.nisp.config.wiring.WSHttp

class BackendConnectorSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val mockHttp: HttpGet = mock[HttpGet]
  
  object BackendConnectorImpl extends BackendConnector {
    override def http: HttpGet = mockHttp
    override def sessionCache: SessionCache = MockSessionCache
    override def serviceUrl: String = "national-insurance"
    override val metricsService: MetricsService = MockMetricsService

    def getNationalInsurance()(implicit headerCarrier: HeaderCarrier): Future[NationalInsuranceRecord] = {
      val urlToRead = s"$serviceUrl/ni"
      retrieveFromCache[NationalInsuranceRecord](APIType.NationalInsurance, urlToRead)(headerCarrier, NationalInsuranceRecord.formats)
    }
  }

  implicit val headerCarrier = HeaderCarrier(extraHeaders = Seq("Accept" -> "application/vnd.hmrc.1.0+json"))

  "connectToMicroservice" should {
    "should return depersonalised JSON" in {
      val json = Json.obj(
        //"qualifyingYears" -> 28,
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

      val Ok = 200
      val response = Future(HttpResponse(Ok, Option.apply(json)))
      when(mockHttp.GET[HttpResponse]("national-insurance/ni")).thenReturn(response)

      val future: Future[NationalInsuranceRecord] = BackendConnectorImpl.getNationalInsurance()

      whenReady(future.failed) {
        t: Throwable =>
          t.getMessage.contains(depersonalisedJson) shouldBe true
          t.getMessage.contains("2016-04-05") shouldBe false
      }
    }
  }

}
