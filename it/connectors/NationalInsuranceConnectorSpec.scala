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
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.nisp.connectors.NationalInsuranceConnector
import uk.gov.hmrc.nisp.models._

import java.time.LocalDate


class NationalInsuranceConnectorSpec
  extends AnyWordSpec
    with WiremockHelper
    with Matchers
    with ScalaFutures
    with GuiceOneAppPerSuite
    with Injecting {

  server.start()

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  implicit val headerCarrier: HeaderCarrier =
    HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.national-insurance.port" -> server.port(),
      "microservice.services.cachable.session-cache.port" -> server.port(),
      "microservice.services.cachable.session-cache.host" -> "localhost"
    )
    .build()

  private val nationalInsuranceConnector: NationalInsuranceConnector =
    inject[NationalInsuranceConnector]

  private val apiUrl: String =
    s"/ni/$nino"

  private val apiGetRequest: RequestPatternBuilder =
    getRequestedFor(urlEqualTo(apiUrl))
      .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))

  private val nationalInsuranceRecord: NationalInsuranceRecord =
    NationalInsuranceRecord(
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

  "getNationalInsurance" should {
    "return the correct response from api" in {
      server.stubFor(
        get(urlEqualTo(apiUrl))
          .willReturn(ok(Json.toJson(nationalInsuranceRecord).toString()))
      )

      whenReady(
        nationalInsuranceConnector.getNationalInsurance(nino)
      ) { response =>
        response shouldBe Right(Right(nationalInsuranceRecord))
      }

      server.verify(1, apiGetRequest)
    }
  }
}
