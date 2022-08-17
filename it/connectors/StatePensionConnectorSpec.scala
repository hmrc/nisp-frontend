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
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.nisp.connectors.StatePensionConnector
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.APIType

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

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

  implicit val headerCarrier: HeaderCarrier =
    HeaderCarrier(sessionId = Some(SessionId("session-sessionId")))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.state-pension.port" -> server.port()
    )
    .build()

  private val nino: Nino =
    Nino("AB123456A")

  private val statePensionConnector: StatePensionConnector =
    inject[StatePensionConnector]

  private val sessionCache: SessionCache =
    inject[SessionCache]

  private val apiRequest: RequestPatternBuilder =
    getRequestedFor(urlEqualTo(s"/ni/$nino"))
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

  override def beforeAll(): Unit = {
    sessionCache.remove().futureValue
    super.beforeEach()
  }

  "getStatePension" should {
    "return the correct response from api" in {
      server.stubFor(
        get(urlEqualTo(s"/ni/$nino"))
          .willReturn(ok(Json.toJson(statePension).toString()))
      )

      whenReady(
        statePensionConnector.getStatePension(nino, delegationState = false)
      ) { statePension =>
        statePension shouldBe statePension
      }

      server.verify(1, apiRequest)
    }

    "return the correct response from cache, and not call api" in {
      sessionCache.cache(APIType.StatePension.toString, statePension).futureValue

      whenReady(
        statePensionConnector.getStatePension(nino, delegationState = false)
      ) { statePension =>
        statePension shouldBe statePension
      }

      server.verify(0, apiRequest)
    }

    "return the correct response from api when user is delegated, and not use cache" in {
      sessionCache.cache(APIType.StatePension.toString, statePension).futureValue

      whenReady(
        statePensionConnector.getStatePension(nino, delegationState = true)
      ) { statePension =>
        statePension shouldBe statePension
      }

      server.verify(1, apiRequest)
    }
  }
}
