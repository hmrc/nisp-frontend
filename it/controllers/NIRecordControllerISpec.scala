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

package controllers

import java.lang.System.currentTimeMillis
import java.time.LocalDate
import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock.{get, ok, post, urlEqualTo}
import it_utils.WiremockHelper
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.domain.{Generator, Nino}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import play.api.test.Helpers.{OK, route, status => getStatus, _}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.models.{NationalInsuranceRecord, StatePension, StatePensionAmountForecast, StatePensionAmountMaximum, StatePensionAmountRegular, StatePensionAmounts}
import uk.gov.hmrc.nisp.models.citizen.{Citizen, CitizenDetailsResponse}
import uk.gov.hmrc.nisp.models.enums.APIType

import scala.concurrent.ExecutionContext.Implicits.global

class NIRecordControllerISpec extends AnyWordSpec
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
      "microservice.services.citizen-details.port" -> server.port(),
      "microservice.services.national-insurance.port" -> server.port(),
      "microservice.services.state-pension.port" -> server.port()
    )
    .build()

  val nino = new Generator().nextNino.nino
  val uuid = UUID.randomUUID()

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
  }

  "showFull" should {
    val request = FakeRequest("GET", s"/check-your-state-pension/account/nirecord")
      .withSession(
        SessionKeys.sessionId -> s"session-$uuid",
        SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
        SessionKeys.authToken -> "Bearer 123"
      )

    "redirect with a full niRecord" in {
      val nationalInsuranceRecord = NationalInsuranceRecord(
        2018,
        1974,
        1,
        1,
        None,
        true,
        LocalDate.now(),
        List(),
        true
      )

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
      val citizen = Citizen(Nino(nino), dateOfBirth = LocalDate.now())
      val citizenDetailsResponse = CitizenDetailsResponse(citizen, None)

      server.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
        .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))

      server.stubFor(get(urlEqualTo(s"/ni/$nino"))
        .willReturn(ok(Json.toJson(statePensionResponse).toString)))

      sessionCache.cache(APIType.StatePension.toString, statePensionResponse).futureValue
      sessionCache.cache(APIType.NationalInsurance.toString, nationalInsuranceRecord).futureValue

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
      result flatMap redirectLocation shouldBe Some("/check-your-state-pension/exclusionni")
    }
    "send an exclusion after the downstream returns one" in {
      val json = Json.parse("""{"code":"EXCLUSION_COPE_PROCESSING_FAILED","copeDataAvailableDate":"2020-01-01"}""")
      val citizen = Citizen(Nino(nino), dateOfBirth = LocalDate.now())
      val citizenDetailsResponse = CitizenDetailsResponse(citizen, None)

      server.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
        .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))

      server.stubFor(get(urlEqualTo(s"/ni/$nino"))
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

    "redirect with gaps in the national insurance record" in {
      val nationalInsuranceRecord = NationalInsuranceRecord(
        2018,
        1974,
        1,
        1,
        None,
        true,
        LocalDate.now(),
        List(),
        true
      )

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
      val citizen = Citizen(Nino(nino), dateOfBirth = LocalDate.now())
      val citizenDetailsResponse = CitizenDetailsResponse(citizen, None)

      server.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
        .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))

      server.stubFor(get(urlEqualTo(s"/ni/$nino"))
        .willReturn(ok(Json.toJson(statePensionResponse).toString)))

      sessionCache.cache(APIType.StatePension.toString, statePensionResponse).futureValue
      sessionCache.cache(APIType.NationalInsurance.toString, nationalInsuranceRecord).futureValue

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
      result flatMap redirectLocation shouldBe Some("/check-your-state-pension/exclusionni")
    }

    "redirect to showFull when the number of gaps in the national insurance response is less than 1" in {
      val nationalInsuranceRecord = NationalInsuranceRecord(
        2018,
        1974,
        0,
        1,
        None,
        true,
        LocalDate.now(),
        List(),
        false
      )

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
      val citizen = Citizen(Nino(nino), dateOfBirth = LocalDate.now())
      val citizenDetailsResponse = CitizenDetailsResponse(citizen, None)

      server.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
        .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))

      server.stubFor(get(urlEqualTo(s"/ni/$nino"))
        .willReturn(ok(Json.toJson(statePensionResponse).toString)))

      sessionCache.cache(APIType.StatePension.toString, statePensionResponse).futureValue
      sessionCache.cache(APIType.NationalInsurance.toString, nationalInsuranceRecord).futureValue

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
      result flatMap redirectLocation shouldBe Some("/check-your-state-pension/account/nirecord")
    }

    "redirect to showFull when the number of gaps in the national insurance response is more than 1" in {
      val nationalInsuranceRecord = NationalInsuranceRecord(
        2018,
        1974,
        2,
        1,
        None,
        true,
        LocalDate.now(),
        List(),
        false
      )

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
      val citizen = Citizen(Nino(nino), dateOfBirth = LocalDate.now())
      val citizenDetailsResponse = CitizenDetailsResponse(citizen, None)

      server.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
        .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))

      server.stubFor(get(urlEqualTo(s"/ni/$nino"))
        .willReturn(ok(Json.toJson(statePensionResponse).toString)))

      sessionCache.cache(APIType.StatePension.toString, statePensionResponse).futureValue
      sessionCache.cache(APIType.NationalInsurance.toString, nationalInsuranceRecord).futureValue

      val result = route(app, request)
      result map getStatus shouldBe Some(OK)
    }
  }

  "pta" should {
    "redirect to show full niRecord" in {
      val request = FakeRequest("GET", s"/check-your-state-pension/account/nirecord/pta")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
          SessionKeys.authToken -> "Bearer 123"
        )

      val nationalInsuranceRecord = NationalInsuranceRecord(
        2018,
        1974,
        1,
        1,
        None,
        true,
        LocalDate.now(),
        List(),
        true
      )

      val citizen = Citizen(Nino(nino), dateOfBirth = LocalDate.now())
      val citizenDetailsResponse = CitizenDetailsResponse(citizen, None)

      server.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
        .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))

      sessionCache.cache(APIType.NationalInsurance.toString, nationalInsuranceRecord).futureValue

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

    "return a 200" in {
      val nationalInsuranceRecord = NationalInsuranceRecord(
        2018,
        1974,
        1,
        1,
        None,
        true,
        LocalDate.now(),
        List(),
        false
      )
      val citizen = Citizen(Nino(nino), dateOfBirth = LocalDate.now())
      val citizenDetailsResponse = CitizenDetailsResponse(citizen, None)

      server.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
        .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))

      sessionCache.cache(APIType.NationalInsurance.toString, nationalInsuranceRecord).futureValue

      val result = route(app, request)

      result map getStatus shouldBe Some(OK)

    }

    "redirect to the exclusion controller when a successful error passed" in {
      val nationalInsuranceRecord = NationalInsuranceRecord(
        2018,
        1974,
        1,
        1,
        None,
        true,
        LocalDate.now(),
        List(),
        true
      )
      val citizen = Citizen(Nino(nino), dateOfBirth = LocalDate.now())
      val citizenDetailsResponse = CitizenDetailsResponse(citizen, None)

      server.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
        .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))

      sessionCache.cache(APIType.NationalInsurance.toString, nationalInsuranceRecord).futureValue

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
    }

    "redirect to the exclusion controller when an error passed" in {

    }
  }

  "showVoluntaryContributions" should {
    "return a 200" in {
      val citizen = Citizen(Nino(nino), dateOfBirth = LocalDate.now())
      val citizenDetailsResponse = CitizenDetailsResponse(citizen, None)

      server.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
        .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))

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
