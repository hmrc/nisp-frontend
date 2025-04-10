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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, route, writeableOf_AnyContentAsEmpty, status => getStatus}
import play.api.test.{FakeRequest, Helpers, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.{LoginTimes, Name}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.PertaxAuthConnector
import uk.gov.hmrc.nisp.controllers.auth._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.citizen.{Citizen, CitizenDetailsResponse}
import uk.gov.hmrc.nisp.models.pertaxAuth.PertaxAuthResponseModel
import uk.gov.hmrc.nisp.utils.Constants.ACCESS_GRANTED
import uk.gov.hmrc.nisp.utils.UnitSpec
import uk.gov.hmrc.nisp.views.Main
import uk.gov.hmrc.nisp.views.html.iv.failurepages.technical_issue

import java.lang.System.currentTimeMillis
import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}


class StatePensionControllerISpec extends UnitSpec
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

  lazy val date: LocalDate = LocalDate.now()
  lazy val instant: Instant = Instant.now()
  lazy val mockPertaxAuthConnector: PertaxAuthConnector = mock[PertaxAuthConnector]
  lazy val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  def mockAuth(pertaxAuthResponseModel: PertaxAuthResponseModel): OngoingStubbing[Future[Either[UpstreamErrorResponse, PertaxAuthResponseModel]]] = {
    when(mockPertaxAuthConnector.authorise(any())(any())).thenReturn(Future.successful(Right(pertaxAuthResponseModel)))
  }

  lazy val authAction = new PertaxAuthActionImpl(
    mockPertaxAuthConnector,
    inject[technical_issue],
    main                = app.injector.instanceOf[Main],
    appConfig           = app.injector.instanceOf[ApplicationConfig]
  )(ExecutionContext.Implicits.global, Helpers.stubMessagesControllerComponents())

  def authenticatedRequest(requestMethod: String = "GET", requestUrl: String = "/"): AuthenticatedRequest[AnyContent] = new AuthenticatedRequest[AnyContent](
    FakeRequest(requestMethod, requestUrl),
    NispAuthedUser(
      Nino("AA000000A"),
      date,
      UserName(Name(Some("John"), Some("Doe"))),
      None, None, isSa = true
    ),
    AuthDetails(ConfidenceLevel.L200, LoginTimes(
      instant, None
    ))
  )

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port" -> wireMockServer.port(),
      "microservice.services.citizen-details.port" -> wireMockServer.port(),
      "microservice.services.state-pension.port" -> server2.port(),
      "microservice.services.national-insurance.port" -> wireMockServer.port(),
      "microservice.services.pertax-auth.port" -> wireMockServer.port()
    )
    .build()

  val citizen: Citizen = Citizen(nino, dateOfBirth = LocalDate.now())
  val citizenDetailsResponse: CitizenDetailsResponse = CitizenDetailsResponse(citizen, None)

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
    val statePensionResponse = StatePension(
      earningsIncludedUpTo = LocalDate.of(2015, 4, 5),
      amounts = StatePensionAmounts(
        protectedPayment = false,
        current = StatePensionAmountRegular(133.41, 580.1, 6961.14),
        forecast = StatePensionAmountForecast(3, 176.76, 690.14, 7657.73),
        maximum = StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
        cope = StatePensionAmountRegular(1, 0, 0)
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

    val nationalInsuranceRecord = NationalInsuranceRecord(
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
  }

  def block: Request[?] => Future[Result] = _ => Future.successful(Ok("Successful"))

  "showCope" should {
    val request = FakeRequest("GET", s"/check-your-state-pension/account/cope")
      .withSession(
        SessionKeys.sessionId -> s"session-$uuid",
        SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
        SessionKeys.authToken -> "Bearer 123"
      )

    "return a 200 when a successful request is sent" in new Test {
      server2.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(statePensionResponse).toString)))

      val result = route(app, request)
      result map getStatus shouldBe Some(OK)
    }

    "redirect to the show state pension page when the state pension returned isn't contracted out" in new Test {
      val contractedOutResponse = statePensionResponse.copy(
        amounts = StatePensionAmounts(
          false,
          StatePensionAmountRegular(0, 580.1, 6961.14),
          StatePensionAmountForecast(3, 0, 690.14, 7657.73),
          StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ))

      server2.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
        .willReturn(ok(Json.toJson(contractedOutResponse).toString)))

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

    "send an exclusion" when {
      "a state pension exclusion is returned" in new Test {

        val json = Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)
        result map getStatus shouldBe Some(SEE_OTHER)
        result flatMap redirectLocation shouldBe Some("/check-your-state-pension/exclusion")
      }

      "a national insurance exclusion is returned" in new Test {

        server2.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
          .willReturn(ok(Json.toJson(statePensionResponse).toString)))

        val json = Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
          .willReturn(forbidden.withBody(json.toString)))

        val result = route(app, request)
        result map getStatus shouldBe Some(SEE_OTHER)
        result flatMap redirectLocation shouldBe Some("/check-your-state-pension/exclusion")
      }
    }

    "return a 200" when {
      "the state pension returned has a mqpscenario that isn't continueWorking" in new Test {
        val mqpResponse = statePensionResponse.copy(
          amounts = StatePensionAmounts(
            false,
            StatePensionAmountRegular(0, 580.1, 6961.14),
            StatePensionAmountForecast(3, 0, 690.14, 7657.73),
            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
            StatePensionAmountRegular(0, 0, 0)
          ))

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
          .willReturn(ok(Json.toJson(nationalInsuranceRecord).toString)))
        server2.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
          .willReturn(ok(Json.toJson(mqpResponse).toString)))

        val result = route(app, request)
        result map getStatus shouldBe Some(OK)
      }

      "a forecast only state pension is returned" in new Test {
        val forecastOnlyResponse = statePensionResponse.copy(amounts = StatePensionAmounts(
          false,
          StatePensionAmountRegular(183.41, 580.1, 6961.14),
          StatePensionAmountForecast(3, 176.76, 690.14, 7657.73),
          StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ))

        wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
          .willReturn(ok(Json.toJson(nationalInsuranceRecord).toString)))
        server2.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
          .willReturn(ok(Json.toJson(forecastOnlyResponse).toString)))

        val result = route(app, request)
        result map getStatus shouldBe Some(OK)
      }

      "a successful standard request is supplied" in new Test {
        wireMockServer.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
          .willReturn(ok(Json.toJson(nationalInsuranceRecord).toString)))
        server2.stubFor(get(urlEqualTo(s"/ni/mdtp/$nino"))
          .willReturn(ok(Json.toJson(statePensionResponse).toString)))

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
