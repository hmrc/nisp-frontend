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

package uk.gov.hmrc.nisp.controllers

import com.github.tomakehurst.wiremock.client.WireMock._
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
import play.api.test.Helpers.{route, status => getStatus, _}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import uk.gov.hmrc.nisp.models._

import java.lang.System.currentTimeMillis
import java.time.LocalDate
import java.util.UUID

class ExclusionControllerISpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with WireMockSupport
  with ScalaFutures
  with BeforeAndAfterEach
  with Injecting {

  wireMockServer.start()

  val nino = "AA123456A"
  val uuid: UUID = UUID.randomUUID()
  val sessionId: String = s"session-$uuid"

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port" -> wireMockServer.port(),
      "microservice.services.identity-verification.port" -> wireMockServer.port(),
      "microservice.services.state-pension.port" -> wireMockServer.port(),
      "microservice.services.national-insurance.port" -> wireMockServer.port()
    )
    .build()

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  override def beforeEach(): Unit = {
    super.beforeEach()

    wireMockServer.stubFor(post(urlEqualTo("/auth/authorise")).willReturn(ok(
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
      earningsIncludedUpTo = LocalDate.of(2015, 4, 5),
      amounts = StatePensionAmounts(
        false,
        StatePensionAmountRegular(133.41, 580.1, 6961.14),
        StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
        StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
        StatePensionAmountRegular(0, 0, 0)
      ),
      pensionAge = 64,
      pensionDate = LocalDate.of(2018, 7, 6),
      finalRelevantYear = "2017",
      numberOfQualifyingYears = 30,
      pensionSharingOrder = false,
      currentFullWeeklyPensionAmount = 155.65,
      reducedRateElection = true,
      statePensionAgeUnderConsideration = false
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
        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(ok(Json.toJson(statePensionResponse).toString)))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }

      "the state pension exclusion reason returned is Dead" in {
        val json = Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "the state pension exclusion reason returned is ManualCorrespondenceIndicator" in {
        val json = Json.parse("""{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message":"The customer needs to contact the National Insurance helpline"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }

      "the state pension exclusion reason returned is CopeProcessingFailed" in {
        val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","message":"The customer needs to contact the National Insurance helpline"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }

      "the state pension exclusion reason is filtered with a cope date and a previous available date" in {
        val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","copeDataAvailableDate":"2020-01-01", "previousAvailableDate": "2019-01-01"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }

      "the state pension exclusion reason is filtered with a cope date" in {
        val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","copeDataAvailableDate":"2020-01-01"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }

      "any other state pension reason is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_CONTRACTED_OUT","message":"The customer needs to contact the National Insurance helpline"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
    }
    "return an internal server error when an error occurs" in {
      wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
        .willReturn(badRequest()))

      val result = route(app, request)

      result map getStatus shouldBe Some(INTERNAL_SERVER_ERROR)
    }

    "redirect the user when they are a non-excluded user" in new Test {

      wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
        .willReturn(ok(Json.toJson(statePensionResponse.copy(reducedRateElection = false)).toString)))

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

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "an exclusion with cope date and no previousAvailableDate is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","copeDataAvailableDate":"2020-01-01"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "Exclusion CopeProcessingFailed is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","message":"The customer needs to contact the National Insurance helpline"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "Exclusion Dead is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "Exclusion ManualCorrespondenceIndicator is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message":"The customer needs to contact the National Insurance helpline"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString())))

        val result = route(app, request)

        result map getStatus shouldBe Some(OK)
      }
      "Any other exclusion is returned" in {
        val json = Json.parse("""{"code":"EXCLUSION_CONTRACTED_OUT","message":"The customer needs to contact the National Insurance helpline"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/$nino"))
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
