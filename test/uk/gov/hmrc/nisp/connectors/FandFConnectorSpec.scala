/*
 * Copyright 2025 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, IM_A_TEAPOT, NOT_FOUND, SERVICE_UNAVAILABLE}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.nisp.config.ApplicationConfig

import scala.concurrent.ExecutionContext
import scala.util.Random

class FandFConnectorSpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with WireMockSupport
  with ScalaFutures
  with BeforeAndAfterEach
  with Injecting {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.fandf.port" -> wireMockServer.port()
    ).build()

  lazy val connector: FandFConnector = new FandFConnector(
    inject[HttpClientV2],
    inject[ApplicationConfig]
  )

  val nino: Nino = Nino(new Generator(new Random()).nextNino.nino)

  val trustedHelper: TrustedHelper = TrustedHelper("principal Name", "attorneyName", "returnLink", Some(nino.nino))

  val fandfTrustedHelperResponse: String = s"""
       |{
       |   "principalName": "principal Name",
       |   "attorneyName": "attorneyName",
       |   "returnLinkUrl": "returnLink",
       |   "principalNino": "$nino"
       |}
       |""".stripMargin

    "Call FandFConnector.getTrustedHelper" should {
      "return a trustedHelper when fandf returns valid json" in {

        wireMockServer.stubFor(get(urlEqualTo("/delegation/get"))
          .willReturn(ok(fandfTrustedHelperResponse)))

        connector.getTrustedHelper().value.futureValue shouldBe Right(trustedHelper)
      }

      Seq(
        SERVICE_UNAVAILABLE,
        IM_A_TEAPOT,
        BAD_REQUEST,
        NOT_FOUND
      ).foreach { statusCode =>
        s"return Left[UpstreamError] when statusCode $statusCode is returned" in {
          wireMockServer.stubFor(get(urlEqualTo("/delegation/get"))
            .willReturn(aResponse.withStatus(statusCode).withBody("")))

          val result = connector.getTrustedHelper().value.futureValue
          val resultMessage = result.left.getOrElse(UpstreamErrorResponse("", 200)).getMessage
          val resultStatus = result.left.getOrElse(UpstreamErrorResponse("", 200)).statusCode
          resultMessage shouldBe s"GET of 'http://localhost:${wireMockServer.port()}/delegation/get' returned $statusCode. Response body: ''"
          resultStatus shouldBe statusCode
        }
      }
    }
}
