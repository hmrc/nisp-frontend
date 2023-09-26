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

import com.github.tomakehurst.wiremock.client.WireMock._
import it_utils.WiremockHelper
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, route, writeableOf_AnyContentAsEmpty, status => getStatus}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.citizen.{Citizen, CitizenDetailsResponse}
import uk.gov.hmrc.nisp.models.pertaxAuth.PertaxAuthResponseModel
import uk.gov.hmrc.nisp.utils.Constants.ACCESS_GRANTED

import java.lang.System.currentTimeMillis
import java.time.LocalDate

class StatePensionControllerISpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with WiremockHelper
  with ScalaFutures
  with BeforeAndAfterEach
  with Injecting {

  server.start()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port" -> server.port(),
      "microservice.services.citizen-details.port" -> server.port(),
      "microservice.services.state-pension.port" -> server.port(),
      "microservice.services.national-insurance.port" -> server.port(),
      "microservice.services.pertax-auth.port" -> server.port()
    )
    .build()

  val citizen = Citizen(nino, dateOfBirth = LocalDate.now())
  val citizenDetailsResponse = CitizenDetailsResponse(citizen, None)

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  override def beforeEach(): Unit = {
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

    server.stubFor(
      get(urlMatching(s"/pertax/$nino/authorise"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(Json.stringify(Json.toJson(PertaxAuthResponseModel(
              ACCESS_GRANTED, "", None, None
            ))))
        )
    )

    server.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
      .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))
  }

  trait Test {
    val statePensionResponse = StatePension(
      LocalDate.of(2015, 4, 5),
      StatePensionAmounts(
        false,
        StatePensionAmountRegular(133.41, 580.1, 6961.14),
        StatePensionAmountForecast(3, 176.76, 690.14, 7657.73),
        StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
        StatePensionAmountRegular(1, 0, 0)
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

  "showCope" should {
    val request = FakeRequest("GET", s"/check-your-state-pension/account/cope")
      .withSession(
        SessionKeys.sessionId -> s"session-$uuid",
        SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
        SessionKeys.authToken -> "Bearer 123"
      )

    "return a 200 when a successful request is sent" in new Test {


      val result = route(app, request)
      result map getStatus shouldBe Some(OK)
    }

    "redirect to the show state pension page when the state pension returned isn't contracted out" in new Test {
//      val contractedOutResponse = statePensionResponse.copy(
//        amounts = StatePensionAmounts(
//          false,
//          StatePensionAmountRegular(0, 580.1, 6961.14),
//          StatePensionAmountForecast(3, 0, 690.14, 7657.73),
//          StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
//          StatePensionAmountRegular(0, 0, 0)
//        ))


      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
      result flatMap redirectLocation shouldBe Some("/check-your-state-pension/account")
    }
  }

  "show" should {
    val request = FakeRequest("GET", s"/check-your-state-pension/account")
      .withSession(
        SessionKeys.sessionId -> s"session-$uuid",
        SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
        SessionKeys.authToken -> "Bearer 123"
      )

//    val nationalInsuranceRecord = NationalInsuranceRecord(
//      2018,
//      1974,
//      1,
//      1,
//      None,
//      true,
//      LocalDate.now(),
//      List(),
//      false
//    )

    "send an exclusion" when {
      "a state pension exclusion is returned" in new Test {

        val json = Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)
        result map getStatus shouldBe Some(SEE_OTHER)
        result flatMap redirectLocation shouldBe Some("/check-your-state-pension/exclusion")
      }

      "a national insurance exclusion is returned" in new Test {


        val json = Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")

        server.stubFor(get(urlEqualTo(s"/ni/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)
        result map getStatus shouldBe Some(SEE_OTHER)
        result flatMap redirectLocation shouldBe Some("/check-your-state-pension/exclusion")
      }
    }

    "return a 200" when {
      "the state pension returned has a mqpscenario that isn't continueWorking" in new Test {
//        val mqpResponse = statePensionResponse.copy(
//          amounts = StatePensionAmounts(
//            false,
//            StatePensionAmountRegular(0, 580.1, 6961.14),
//            StatePensionAmountForecast(3, 0, 690.14, 7657.73),
//            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
//            StatePensionAmountRegular(0, 0, 0)
//          ))

        server.stubFor(get(urlEqualTo(s"/citizen-details/$nino/designatory-details"))
          .willReturn(ok(Json.toJson(citizenDetailsResponse).toString)))


        val result = route(app, request)
        result map getStatus shouldBe Some(OK)
      }

      "a forecast only state pension is returned" in new Test {
//        val forecastOnlyResponse = statePensionResponse.copy(amounts = StatePensionAmounts(
//          false,
//          StatePensionAmountRegular(183.41, 580.1, 6961.14),
//          StatePensionAmountForecast(3, 176.76, 690.14, 7657.73),
//          StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
//          StatePensionAmountRegular(0, 0, 0)
//        ))


        val result = route(app, request)
        result map getStatus shouldBe Some(OK)
      }
      "a successful standard request is supplied" in new Test {


        val result = route(app, request)
        result map getStatus shouldBe Some(OK)
      }
    }
  }

  "pta" should {
    "redirect to the show state pension page when hit" in {
      val request = FakeRequest("GET", s"/check-your-state-pension/account/pta")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
          SessionKeys.authToken -> "Bearer 123"
        )

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
      result flatMap redirectLocation shouldBe Some("/check-your-state-pension/account")
    }
  }

  "signOut" should {
    "redirect to the feedback frontend page when hit" in {
      val request = FakeRequest("GET", s"/check-your-state-pension/sign-out")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
          SessionKeys.authToken -> "Bearer 123"
        )

      val result = route(app, request)
      result map getStatus shouldBe Some(SEE_OTHER)
    }
  }

  "timeout" should {
    "return a 200 when hitting a timeout" in {
      val request = FakeRequest("GET", s"/check-your-state-pension/timeout")
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
