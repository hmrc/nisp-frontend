/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import java.lang.System.currentTimeMillis
import java.time.LocalDate
import com.github.tomakehurst.wiremock.client.WireMock._
import it_utils.WiremockHelper
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers.{route, status => getStatus, _}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.{StatePension, StatePensionAmountForecast, StatePensionAmountMaximum, StatePensionAmountRegular, StatePensionAmounts}

import scala.concurrent.ExecutionContext.Implicits.global

class ExclusionControllerISpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with WiremockHelper
  with ScalaFutures
  with BeforeAndAfterEach
  with Injecting {

  server.start()

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port" -> server.port(),
      "microservice.services.identity-verification.port" -> server.port(),
      "microservice.services.state-pension.port" -> server.port(),
      "microservice.services.national-insurance.port" -> server.port()
    )
    .build()

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-$uuid")))
  private val sessionCache: SessionCache = inject[SessionCache]

  override def beforeEach(): Unit = {
    sessionCache.remove().futureValue
    super.beforeEach()

    server.stubFor(post(urlEqualTo("/auth/authorise")).willReturn(ok(
      s"""{
         |"nino": "$nino",
         |"confidenceLevel": 200,
         |"loginTimes": {
         |  "currentLogin": "2021-06-07T10:52:02.594Z",
         |  "previousLogin": "2021-06-07T10:52:02.594Z"
         |  },
         |"allEnrolments": ""
         |}
      """.stripMargin)))
  }

  trait Test {
    val statePensionResponse = StatePension(
      LocalDate.of(2015, 4, 5),
      StatePensionAmounts(
        false,
        StatePensionAmountRegular(133.41, 580.1, 6961.14),
        StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
        StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
        StatePensionAmountRegular(0, 0, 0)
      ),
      64,
      LocalDate.of(2018, 7, 6),
      "2017",
      30,
      false,
      155.65,
      true,
      false
    )
  }

  "showSP" should {
    val request = FakeRequest("GET", s"/check-your-state-pension/exclusion")
      .withSession(
        SessionKeys.sessionId -> s"session-$uuid",
        SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
        SessionKeys.authToken -> "Bearer 123"
      )

    "return a 200" when {
      "there is a success and reducedRateElection is true" in new Test {

        sessionCache.cache(APIType.StatePension.toString, statePensionResponse).futureValue

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }

      "the state pension exclusion reason returned is Dead" in {
        val json = Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "the state pension exclusion reason returned is ManualCorrespondenceIndicator" in {
        val json = Json.parse("""{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message":"The customer needs to contact the National Insurance helpline"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }

      "the state pension exclusion reason returned is CopeProcessingFailed" in {
        val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","message":"The customer needs to contact the National Insurance helpline"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }

      "the state pension exclusion reason is filtered with a cope date and a previous available date" in {
        val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","copeDataAvailableDate":"2020-01-01", "previousAvailableDate": "2019-01-01"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }

      "the state pension exclusion reason is filtered with a cope date" in {
        val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","copeDataAvailableDate":"2020-01-01"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }

      "any other state pension reason is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_CONTRACTED_OUT","message":"The customer needs to contact the National Insurance helpline"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
    }
    "return an internal server error when an error occurs" in {
      server.stubFor(get(urlEqualTo(s"/ni/$nino"))
        .willReturn(badRequest()))

      val result = route(app, request)

      result map getStatus shouldBe Some(INTERNAL_SERVER_ERROR)
    }

    "redirect the user when they are a non-excluded user" in new Test {

      sessionCache.cache(APIType.StatePension.toString, statePensionResponse.copy(reducedRateElection = false)).futureValue

      val result = route(app, request)

      result map getStatus shouldBe Some(SEE_OTHER)
    }
  }

  "showNI" should {
    val request = FakeRequest("GET", s"/check-your-state-pension/exclusionni")
      .withSession(
        SessionKeys.sessionId -> s"session-$uuid",
        SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
        SessionKeys.authToken -> "Bearer 123"
      )

    "return a 200" when {
      "an exclusion with cope date and previousAvailableDate is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","copeDataAvailableDate":"2020-01-01", "previousAvailableDate": "2019-01-01"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "an exclusion with cope date and no previousAvailableDate is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","copeDataAvailableDate":"2020-01-01"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "Exclusion CopeProcessingFailed is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","message":"The customer needs to contact the National Insurance helpline"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "Exclusion Dead is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "Exclusion ManualCorrespondenceIndicator is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message":"The customer needs to contact the National Insurance helpline"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "Any other exclusion is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_CONTRACTED_OUT","message":"The customer needs to contact the National Insurance helpline"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
    }

    "redirect to showGaps page if there is no exclusion returned" in {
      val result = route(app, request)

      result map getStatus shouldBe Some(SEE_OTHER)
    }
  }
}
