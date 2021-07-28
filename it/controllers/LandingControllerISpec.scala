package controllers

import java.time.LocalDate
import java.util.UUID

import it_utils.{FakeCachedStaticHtmlPartialRetriever, FakePartialRetriever, FakeTemplateRenderer, WiremockHelper}
import com.github.tomakehurst.wiremock.client.WireMock.{forbidden, get, notFound, ok, post, serviceUnavailable, urlEqualTo}
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils.currentTimeMillis
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.{status => getStatus, _}
import play.api.test.Helpers.route
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.libs.json.{Format, JsValue, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.nisp.connectors.IdentityVerificationSuccessResponse
import uk.gov.hmrc.nisp.models.{NationalInsuranceRecord, NationalInsuranceTaxYear, StatePension, StatePensionAmountForecast, StatePensionAmountMaximum, StatePensionAmountRegular, StatePensionAmounts}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

class LandingControllerISpec extends WordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with WiremockHelper
  with ScalaFutures
  with MockitoSugar
  with BeforeAndAfterEach {

  implicit val formats: Format[IdentityVerificationSuccessResponse] = Json.format[IdentityVerificationSuccessResponse]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TemplateRenderer].toInstance(FakeTemplateRenderer),
      bind[FormPartialRetriever].to[FakePartialRetriever],
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever)
    )
    .configure(
      "microservice.services.auth.port" -> server.port(),
      "microservice.services.identity-verification.port" -> server.port()
    )
    .build()


  val nino = new Generator().nextNino.nino
  val uuid = UUID.randomUUID()

  val returnJson = Json.toJson(StatePension(
    LocalDate.of(2015, 4, 5),
    StatePensionAmounts(
      false,
      StatePensionAmountRegular(133.41, 580.1, 6961.14),
      StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
      StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
      StatePensionAmountRegular(0, 0, 0)
    ),
    64, LocalDate.of(2018, 7, 6), "2017-18", 30, false, 155.65,
    false,
    false
  ))

  val nationalInsuaranceTaxYear = NationalInsuranceTaxYear("2011-12", true, 0.0, 1, 1, 1, 0.0, None, None, true, false)
  val nationalInsuranceRecord = NationalInsuranceRecord(2,2,0,0,None, false, LocalDate.now(), List(nationalInsuaranceTaxYear), false)
  val nationalInsuranceJson: JsValue = Json.toJson(nationalInsuranceRecord)

  override def beforeEach(): Unit = {
    super.beforeEach()

    server.stubFor(post(urlEqualTo("/auth/authorise")).willReturn(ok(
      s"""{
         |"nino": "$nino",
         |"confidenceLevel": 200,
         |"loginTimes": {
         |  "currentLogin": "${DateTime.now}",
         |  "previousLogin": "${DateTime.now}"
         |  }
         |}
      """.stripMargin)))
  }

  "showNotAuthorised" should {
    List(
      "Incomplete",
      "FailedMatching",
      "InsufficientEvidence",
      "UserAborted",
      "Timeout",
      "PreconditionFailed",
      "FailedIV"
    ) foreach { journeyId =>
      s"return 401 with $journeyId as journeyId" in {

        server.stubFor(get(urlEqualTo(s"/mdtp/journey/journeyId/$journeyId"))
          .willReturn(ok(Json.toJson(IdentityVerificationSuccessResponse(journeyId)).toString())))

        val request = FakeRequest("GET", s"/check-your-state-pension/not-authorised?journeyId=$journeyId")
          .withSession(
            SessionKeys.sessionId -> s"session-$uuid",
            SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString
          )

        val result = route(app, request)

        result map (getStatus) shouldBe Some(UNAUTHORIZED)
      }
    }

    "return 423 when LockedOut is returned from IV" in {
      server.stubFor(get(urlEqualTo(s"/mdtp/journey/journeyId/LockedOut"))
        .willReturn(ok(Json.toJson(IdentityVerificationSuccessResponse("LockedOut")).toString())))

      val request = FakeRequest("GET", s"/check-your-state-pension/not-authorised?journeyId=LockedOut")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString
        )

      val result = route(app, request)

      result map (getStatus) shouldBe Some(LOCKED)
    }

    "return 500 when TechnicalIssue is returned from IV" in {
      server.stubFor(get(urlEqualTo(s"/mdtp/journey/journeyId/TechnicalIssue"))
        .willReturn(ok(Json.toJson(IdentityVerificationSuccessResponse("TechnicalIssue")).toString())))

      val request = FakeRequest("GET", s"/check-your-state-pension/not-authorised?journeyId=TechnicalIssue")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString
        )

      val result = route(app, request)

      result map (getStatus) shouldBe Some(INTERNAL_SERVER_ERROR)
    }

    "return 500 when 404 is returned from IV" in {
      server.stubFor(get(urlEqualTo(s"/mdtp/journey/journeyId/journeyId"))
        .willReturn(notFound()))

      val request = FakeRequest("GET", "/check-your-state-pension/not-authorised?journeyId=journeyId")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString
        )

      val result = route(app, request)

      result map (getStatus) shouldBe Some(INTERNAL_SERVER_ERROR)
    }

    "return 500 when 403 is returned from IV" in {
      server.stubFor(get(urlEqualTo(s"/mdtp/journey/journeyId/journeyId"))
        .willReturn(forbidden()))

      val request = FakeRequest("GET", "/check-your-state-pension/not-authorised?journeyId=journeyId")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString
        )

      val result = route(app, request)

      result map (getStatus) shouldBe Some(INTERNAL_SERVER_ERROR)
    }

    "return 500 when 503 is returned from IV" in {
      server.stubFor(get(urlEqualTo(s"/mdtp/journey/journeyId/journeyId"))
        .willReturn(serviceUnavailable()))

      val request = FakeRequest("GET", "/check-your-state-pension/not-authorised?journeyId=journeyId")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString
        )

      val result = route(app, request)

      result map (getStatus) shouldBe Some(INTERNAL_SERVER_ERROR)
    }
  }
}