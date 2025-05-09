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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.nisp.models._

import java.time.LocalDate
import java.util.UUID


class StatePensionConnectorSpec
  extends AnyWordSpec
    with WireMockSupport
    with Matchers
    with ScalaFutures
    with GuiceOneAppPerSuite
    with Injecting {


  val uuid: UUID = UUID.randomUUID()
  val sessionId: String = s"session-$uuid"
  val nino = Nino("AA123456A")

  wireMockServer.start()

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  implicit val headerCarrier: HeaderCarrier =
    HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.state-pension.port" -> wireMockServer.port(),
    )
    .build()

  private val statePensionConnector: StatePensionConnector =
    inject[StatePensionConnector]

  private val apiUrl: String =
    s"/ni/mdtp/$nino"

  private val apiGetRequest: RequestPatternBuilder =
    getRequestedFor(urlEqualTo(apiUrl))
      .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))

  private val statePension: StatePension =
    StatePension(
      earningsIncludedUpTo = LocalDate.of(2015, 4, 5),
      amounts = StatePensionAmounts(
        protectedPayment = false,
        current = StatePensionAmountRegular(133.41, 580.1, 6961.14),
        forecast = StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
        maximum = StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
        cope = StatePensionAmountRegular(0, 0, 0)
      ),
      pensionAge = 64,
      pensionDate = LocalDate.of(2018, 7, 6),
      finalRelevantYear = "2017-18",
      numberOfQualifyingYears = 30,
      pensionSharingOrder = false,
      currentFullWeeklyPensionAmount = 155.65,
      reducedRateElection = false,
      statePensionAgeUnderConsideration = false
    )

  "getStatePension" should {
    "return the correct response from api" in {
      wireMockServer.stubFor(
        get(urlEqualTo(apiUrl))
          .willReturn(ok(Json.toJson(statePension).toString()))
      )

      whenReady(
        statePensionConnector.getStatePension(nino)
      ) { response =>
        response shouldBe Right(Right(statePension))
      }

      wireMockServer.verify(1, apiGetRequest)
    }
  }
}
