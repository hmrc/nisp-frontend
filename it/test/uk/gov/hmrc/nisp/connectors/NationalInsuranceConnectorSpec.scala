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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.nisp.connectors.NationalInsuranceConnector
import uk.gov.hmrc.nisp.it_utils.WiremockHelper
import uk.gov.hmrc.nisp.models._

import java.time.LocalDate


class NationalInsuranceConnectorSpec
  extends AnyWordSpec
    with WiremockHelper
    with Matchers
    with ScalaFutures
    with GuiceOneAppPerSuite
    with Injecting {

  server.start()

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  implicit val headerCarrier: HeaderCarrier =
    HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.national-insurance.port" -> server.port(),
      "microservice.services.cachable.session-cache.port" -> server.port(),
      "microservice.services.cachable.session-cache.host" -> "localhost"
    )
    .build()

  private val nationalInsuranceConnector: NationalInsuranceConnector =
    inject[NationalInsuranceConnector]

  private val apiUrl: String =
    s"/ni/$nino"

  private val apiGetRequest: RequestPatternBuilder =
    getRequestedFor(urlEqualTo(apiUrl))
      .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))

  private val nationalInsuranceRecord: NationalInsuranceRecord =
    NationalInsuranceRecord(
      qualifyingYears = 2018,
      qualifyingYearsPriorTo1975 = 1974,
      numberOfGaps = 1,
      numberOfGapsPayable = 1,
      dateOfEntry = None,
      homeResponsibilitiesProtection = true,
      earningsIncludedUpTo = LocalDate.now(),
      taxYears = List(),
      reducedRateElection = false
    )

  "getNationalInsurance" should {
    "return the correct response from api" in {
      server.stubFor(
        get(urlEqualTo(apiUrl))
          .willReturn(ok(Json.toJson(nationalInsuranceRecord).toString()))
      )

      whenReady(
        nationalInsuranceConnector.getNationalInsurance(nino)
      ) { response =>
        response shouldBe Right(Right(nationalInsuranceRecord))
      }

      server.verify(1, apiGetRequest)
    }
  }
}
