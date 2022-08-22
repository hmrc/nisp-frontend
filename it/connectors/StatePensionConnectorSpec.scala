package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import it_utils.WiremockHelper
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
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.nisp.connectors.StatePensionConnector
import uk.gov.hmrc.nisp.models._

import java.time.LocalDate
import java.util.UUID

class StatePensionConnectorSpec
  extends AnyWordSpec
    with WiremockHelper
    with Matchers
    with ScalaFutures
    with GuiceOneAppPerSuite
    with Injecting {

  server.start()

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val uuid: UUID =
    UUID.randomUUID()

  implicit val headerCarrier: HeaderCarrier =
    HeaderCarrier(sessionId = Some(SessionId(s"session-$uuid")))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.state-pension.port" -> server.port(),
      "microservice.services.cachable.session-cache.port" -> server.port()
    )
    .build()

  private val nino: Nino =
    Nino("AB123456A")

  private val apiUrl: String =
    s"/ni/$nino"

  private val cacheUrl: String =
    s"/keystore/nisp-frontend/session-$uuid"

  private val cachePutUrl: String =
    s"$cacheUrl/data/StatePension"



  private val statePensionConnector: StatePensionConnector =
    inject[StatePensionConnector]

  private val apiGetRequest: RequestPatternBuilder =
    getRequestedFor(urlEqualTo(apiUrl))
      .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))

  private val cacheGetRequest: RequestPatternBuilder =
    getRequestedFor(urlEqualTo(cacheUrl))

  private val cachePutRequest: RequestPatternBuilder =
    putRequestedFor(urlEqualTo(cachePutUrl))

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
      server.stubFor(
        get(urlEqualTo(cacheUrl))
          .willReturn(notFound())
      )

      server.stubFor(
        get(urlEqualTo(apiUrl))
          .willReturn(ok(Json.toJson(statePension).toString()))
      )

      server.stubFor(
        put(urlEqualTo(cachePutUrl))
          .withRequestBody(
            equalToJson(
              Json.toJson(Json.obj(
                "name" -> "StatePension",
                "result" -> Json.toJson(statePension)
              )).toString() )
          )
          .willReturn(ok(Json.toJson(statePension).toString()))
      )

      whenReady(
        statePensionConnector.getStatePension(
          nino = nino,
          delegationState = false
        )
      ) { response =>
        response shouldBe Right(Right(statePension))
      }

      server.verify(1, apiGetRequest)
      server.verify(1, cacheGetRequest)
      server.verify(1, cachePutRequest)
    }

    "return the correct response from cache, and not call api" in {
      server.stubFor(
        get(urlEqualTo(cacheUrl))
          .willReturn(ok(Json.obj(
            "id" -> uuid,
            "data" -> Json.toJson(statePension)
          ).toString()))
      )

      whenReady(
        statePensionConnector.getStatePension(
          nino = nino,
          delegationState = false
        )
      ) { response =>
        response shouldBe Right(Right(statePension))
      }

      server.verify(0, apiGetRequest)
      server.verify(1, cacheGetRequest)
      server.verify(0, cachePutRequest)
    }

    "return the correct response from api when user is delegated, and not use cache" in {
      server.stubFor(
        get(urlEqualTo(apiUrl))
          .willReturn(ok(Json.toJson(statePension).toString()))
      )

      whenReady(
        statePensionConnector.getStatePension(
          nino = nino,
          delegationState = true
        )
      ) { response =>
        response shouldBe Right(Right(statePension))
      }

      server.verify(1, apiGetRequest)
      server.verify(0, cacheGetRequest)
      server.verify(0, cachePutRequest)
    }
  }
}
