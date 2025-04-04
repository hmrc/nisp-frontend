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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{route, status => getStatus, _}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.citizen.{Citizen, CitizenDetailsResponse}
import uk.gov.hmrc.nisp.models.pertaxAuth.PertaxAuthResponseModel
import uk.gov.hmrc.nisp.utils.Constants.ACCESS_GRANTED

import java.lang.System.currentTimeMillis
import java.time.LocalDate
import java.util.UUID

class NIRecordControllerISpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with WireMockSupport
  with ScalaFutures
  with BeforeAndAfterEach
  with Injecting {

  val uuid: UUID = UUID.randomUUID()
  val sessionId: String = s"session-$uuid"
  val nino = Nino("AA123456A")
  val server2: WireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  wireMockServer.start()
  server2.start()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port" -> wireMockServer.port(),
      "microservice.services.citizen-details.port" -> wireMockServer.port(),
      "microservice.services.national-insurance.port" -> wireMockServer.port(),
      "microservice.services.state-pension.port" -> server2.port(),
      "microservice.services.pertax-auth.port" -> wireMockServer.port()
    )
    .build()

  val citizen = Citizen(nino, dateOfBirth = LocalDate.now())
  val citizenDetailsResponse = CitizenDetailsResponse(citizen, None)
  lazy val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-$uuid")))
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

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
         |  "allEnrolments": [{
         |    "key": "IR-SA",
         |    "identifiers": [
         |      { "key": "TaxOfficeNumber", "value": "123" },
         |      { "key": "TaxOfficeReference", "value": "AB12345" }
         |    ],
         |    "state": "Activated"
         |  }],
         |  "authProviderId": {
         |    "paClientId": "123"
         |  },
         |  "optionalCredentials": {
         |    "providerId": "123",
         |    "providerType": "paClientId"
         |  }
         |}
      """.stripMargin)))

    wireMockServer.stubFor(
      post(urlMatching("/pertax/authorise"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(Json.stringify(Json.toJson(PertaxAuthResponseModel(
              ACCESS_GRANTED, "", None, None
            ))))
        )
    )

    wireMockServer.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
      .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))
  }

  trait Test {
    val nationalInsuranceRecord = NationalInsuranceRecord(
      qualifyingYears = 2018,
      qualifyingYearsPriorTo1975 = 1974,
      numberOfGaps = 1,
      numberOfGapsPayable = 1,
      dateOfEntry = None,
      homeResponsibilitiesProtection = true,
      earningsIncludedUpTo = LocalDate.now(),
      taxYears = List(),
      reducedRateElection = true
    )

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

  "showFull" should {
    val request = FakeRequest("GET", s"/check-your-state-pension/account/nirecord")
      .withSession(
        SessionKeys.sessionId -> s"session-$uuid",
        SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
        SessionKeys.authToken -> "Bearer 123"
      )

    "redirect with a full niRecord" in new Test {
      server2.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(statePensionResponse).toString)))

      wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(nationalInsuranceRecord).toString)))

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
      result flatMap redirectLocation shouldBe Some("/check-your-state-pension/exclusionni")
    }
    "send an exclusion after the downstream returns one" in new Test {
      val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","copeDataAvailableDate":"2020-01-01"}""")

      wireMockServer.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
        .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))

      wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(json).toString)))

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
      result flatMap redirectLocation shouldBe Some("/check-your-state-pension/exclusionni")
    }
  }

  "showGaps" should {
    val request = FakeRequest("GET", s"/check-your-state-pension/account/nirecord/gaps")
      .withSession(
        SessionKeys.sessionId -> s"session-$uuid",
        SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
        SessionKeys.authToken -> "Bearer 123"
      )

    "redirect with gaps in the national insurance record" in new Test {

      wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(nationalInsuranceRecord).toString)))
      server2.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(statePensionResponse).toString)))

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
      result flatMap redirectLocation shouldBe Some("/check-your-state-pension/exclusionni")
    }

    "redirect to showFull when the number of gaps in the national insurance response is less than 1" in new Test {
      wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(nationalInsuranceRecord.copy(numberOfGaps = 0, reducedRateElection = false)).toString)))
      server2.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(statePensionResponse).toString)))

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
      result flatMap redirectLocation shouldBe Some("/check-your-state-pension/account/nirecord")
    }

    "redirect to showFull when the number of gaps in the national insurance response is more than 1" in new Test {
      wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(statePensionResponse).toString)))
      wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(nationalInsuranceRecord.copy(numberOfGaps = 2, reducedRateElection = false)).toString)))

      val result = route(app, request)
      result map getStatus shouldBe Some(OK)
    }
  }

  "pta" should {
    "redirect to show full niRecord" in new Test {
      val request = FakeRequest("GET", s"/check-your-state-pension/account/nirecord/pta")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
          SessionKeys.authToken -> "Bearer 123"
        )

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
      result flatMap redirectLocation shouldBe Some("/check-your-state-pension/account/nirecord")
    }

  }

  "showGapsAndHowToCheckThem" should {
    val request = FakeRequest("GET", s"/check-your-state-pension/account/nirecord/gapsandhowtocheck")
      .withSession(
        SessionKeys.sessionId -> s"session-$uuid",
        SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
        SessionKeys.authToken -> "Bearer 123"
      )

    "return a 200" in new Test {
      wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(nationalInsuranceRecord.copy(reducedRateElection = false)).toString)))

      val result = route(app, request)

      result map getStatus shouldBe Some(OK)

    }

    "redirect to the exclusion controller when a successful error passed" in new Test {
      wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(nationalInsuranceRecord).toString)))

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
    }
  }

  "showVoluntaryContributions" should {
    "return a 200" in new Test {

      val request = FakeRequest("GET", s"/check-your-state-pension/account/nirecord/voluntarycontribs")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
          SessionKeys.authToken -> "Bearer 123"
        )

      val result = route(app, request)

      result map getStatus shouldBe Some(OK)
    }
  }
}
