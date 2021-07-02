/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.connectors

import java.time.LocalDate

import org.mockito.Mockito
import org.mockito.Mockito.reset
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, forbidden, get, getRequestedFor, ok, urlEqualTo, matching}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, Upstream4xxResponse}
import uk.gov.hmrc.nisp.helpers.{FakeSessionCache, TestAccountBuilder}
import uk.gov.hmrc.nisp.models.{Exclusion, _}
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.WireMockHelper
import uk.gov.hmrc.play.test.UnitSpec

class StatePensionConnectorSpec extends UnitSpec with ScalaFutures with MockitoSugar with GuiceOneAppPerSuite with
  Injecting with BeforeAndAfterEach with WireMockHelper {

  implicit val headerCarrier = HeaderCarrier()

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val mockMetricService = mock[MetricsService](Mockito.RETURNS_DEEP_STUBS)
  val uuidRegex =  """[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}"""

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MetricsService].toInstance(mockMetricService),
      bind[SessionCache].toInstance(FakeSessionCache)
    )
    .configure(
      "microservice.services.state-pension.port" -> server.port()
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetricService)
  }

  val nino = TestAccountBuilder.regularNino

  lazy val statePensionConnector = inject[StatePensionConnector]

  "getStatePension" should {
    "return the correct response for the regular test user" in {

      val expectedStatePension = TestAccountBuilder.getRawJson(nino, "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/$nino"))
          .willReturn(ok(Json.toJson(expectedStatePension).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.regularNino)) { statePension =>
        statePension shouldBe Right(StatePension(
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
      }
    }

    "return a failed Future 403 with a Dead message for all exclusion" in {

      val json = Json.toJson("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"""")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.excludedAll}"))
          .willReturn(forbidden()
            .withBody(json.toString)
          )
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.excludedAll).failed) {
        case ex: Upstream4xxResponse =>
          ex.upstreamResponseCode shouldBe 403
          ex.message.contains("EXCLUSION_DEAD") shouldBe true
      }

      server.verify(getRequestedFor(urlEqualTo(s"/ni/${TestAccountBuilder.excludedAll}"))
        .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
        .withHeader(HeaderNames.xRequestId, equalTo("-"))
        .withHeader(HeaderNames.xSessionId, equalTo("-"))
        .withHeader("CorrelationId", matching(uuidRegex))
      )
    }

    "return a failed Future 403 with a MCI message for all exclusion but dead" in {
      val json = Json.toJson("""{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message":"The customer needs to contact the National Insurance helpline"""")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.excludedAllButDead}"))
          .willReturn(forbidden().withBody(json.toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.excludedAllButDead).failed) {
        case ex: Upstream4xxResponse =>
          ex.upstreamResponseCode shouldBe 403
          ex.message.contains("EXCLUSION_MANUAL_CORRESPONDENCE") shouldBe true
      }
    }

    "return the correct list of exclusions for the the user with all but dead and MCI" in {

      val expectedExclusion = TestAccountBuilder.getRawJson(TestAccountBuilder.excludedAllButDeadMCI, "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.excludedAllButDeadMCI}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.excludedAllButDeadMCI)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.PostStatePensionAge,
            Exclusion.AmountDissonance,
            Exclusion.MarriedWomenReducedRateElection,
            Exclusion.IsleOfMan
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(false)
        ))
      }
    }

    "return the state pension age flag as true for a user with Amount Dis exclusion with a true flag for incoming State Pension Age Under Consideration" in {

      val expectedExclusion = TestAccountBuilder.getRawJson(
        TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino,
        "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.AmountDissonance
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        ))
      }
    }

    "return the state pension age flag as true for a user with IOM exclusion with a true flag for incoming State Pension Age Under Consideration" in {

      val expectedExclusion = TestAccountBuilder.getRawJson(
        TestAccountBuilder.spaUnderConsiderationExclusionIoMNino,
        "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionIoMNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionIoMNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.IsleOfMan
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        ))
      }
    }

    "return the state pension age flag as true for a user with MWRRE exclusion with a true flag for incoming State Pension Age Under Consideration" in {
      val expectedExclusion = TestAccountBuilder.getRawJson(
        TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino,
        "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.MarriedWomenReducedRateElection
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        ))
      }
    }

    "return the state pension age flag as true for a user with Over SPA exclusion with a true flag for incoming State Pension Age Under Consideration" in {
      val expectedExclusion = TestAccountBuilder.getRawJson(
        TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino,
        "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.PostStatePensionAge
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        ))
      }
    }

    "return the state pension age flag as true for a user with Multiple exclusions with a true flag for incoming State Pension Age Under Consideration" in {
      val expectedExclusion = TestAccountBuilder.getRawJson(
        TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino,
        "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.PostStatePensionAge,
            Exclusion.AmountDissonance,
            Exclusion.MarriedWomenReducedRateElection,
            Exclusion.IsleOfMan
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        ))
      }
    }

    "return the state pension age flag as none for a user with exclusion with no flag for incoming State Pension Age Under Consideration" in {
      val expectedExclusion = TestAccountBuilder.getRawJson(
        TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino,
        "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.IsleOfMan
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = None
        ))
      }
    }
  }
}
