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

package uk.gov.hmrc.nisp.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.Mockito
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Injecting
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.helpers.{FakeSessionCache, TestAccountBuilder}
import uk.gov.hmrc.nisp.models.StatePensionExclusion.{ForbiddenStatePensionExclusion, OkStatePensionExclusion}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.{UnitSpec, WireMockHelper}

import java.time.LocalDate

class StatePensionConnectorSpec
  extends UnitSpec
    with ScalaFutures
    with GuiceOneAppPerSuite
    with Injecting
    with BeforeAndAfterEach
    with WireMockHelper {

  implicit val headerCarrier: HeaderCarrier =
    HeaderCarrier()

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  val mockMetricService: MetricsService =
    mock[MetricsService](Mockito.RETURNS_DEEP_STUBS)
  val uuidRegex =
    """[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}"""

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MetricsService].toInstance(mockMetricService),
      bind[SessionCache].toInstance(FakeSessionCache)
    )
    .configure(
      "microservice.services.state-pension.port" -> server.port()
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetricService)
  }

  val nino: Nino =
    TestAccountBuilder.regularNino

  lazy val statePensionConnector: StatePensionConnector =
    inject[StatePensionConnector]

  val expectedStatePension: JsValue =
    TestAccountBuilder.getRawJson(nino, "state-pension")

  val statePension: StatePension =
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
    "return the correct response for the regular test user" in {
      server.stubFor(
        get(urlEqualTo(s"/ni/$nino"))
          .willReturn(ok(Json.toJson(expectedStatePension).toString()))
      )

      whenReady(
        statePensionConnector.getStatePension(TestAccountBuilder.regularNino, delegationState = false)
      ) { response =>
        response shouldBe Right(Right(statePension))
      }

      server.verify(
        getRequestedFor(urlEqualTo(s"/ni/$nino"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
      )

    }

    "return the correct response for the regular test user when delegationState is true" in {
      server.stubFor(
        get(urlEqualTo(s"/ni/$nino"))
          .willReturn(ok(Json.toJson(expectedStatePension).toString()))
      )

      whenReady(
        statePensionConnector.getStatePension(TestAccountBuilder.regularNino, delegationState = true)
      ) { response =>
        response shouldBe Right(Right(statePension))
      }

      server.verify(
        getRequestedFor(urlEqualTo(s"/ni/$nino"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
      )
    }

    "return a failed Future 403 with a Dead message for all exclusion" in {

      val json = Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.excludedAll}"))
          .willReturn(
            forbidden()
              .withBody(json.toString)
          )
      )

      val result = await(statePensionConnector.getStatePension(TestAccountBuilder.excludedAll, delegationState = false))

      result.map {
        spExclusion =>
          val exclusion = spExclusion.left.get.asInstanceOf[ForbiddenStatePensionExclusion]

          exclusion.code shouldBe Exclusion.Dead
      }
    }

    "return a failed Future 403 with a MCI message for all exclusion but dead" in {
      val json = Json.parse("""{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message":"The customer needs to contact the National Insurance helpline"}""")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.excludedAllButDead}"))
          .willReturn(forbidden().withBody(json.toString()))
      )

      val result = await(statePensionConnector.getStatePension(TestAccountBuilder.excludedAllButDead, delegationState = false))

      result.map {
        spExclusion =>
          val exclusion = spExclusion.left.get.asInstanceOf[ForbiddenStatePensionExclusion]

          exclusion.code shouldBe Exclusion.ManualCorrespondenceIndicator
      }
    }

    "return the correct list of exclusions for the the user with all but dead and MCI" in {

      val expectedExclusion = TestAccountBuilder.getRawJson(TestAccountBuilder.excludedAllButDeadMCI, "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.excludedAllButDeadMCI}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.excludedAllButDeadMCI, delegationState = false)) { result =>
        result shouldBe Right(Left(OkStatePensionExclusion(
          List(
            Exclusion.PostStatePensionAge,
            Exclusion.AmountDissonance,
            Exclusion.MarriedWomenReducedRateElection,
            Exclusion.IsleOfMan
          ),
          Some(65),
          Some(LocalDate.of(2017, 7, 18)),
          Some(false)
        )))
      }
    }

    "return the state pension age flag as true for a user with Amount Dis exclusion with a true flag for incoming State Pension Age Under Consideration" in {

      val expectedExclusion =
        TestAccountBuilder.getRawJson(TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino, "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino, delegationState = false)) { result =>
        result shouldBe Right(Left(OkStatePensionExclusion(
          List(
            Exclusion.AmountDissonance
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        )))
      }
    }

    "return the state pension age flag as true for a user with IOM exclusion with a true flag for incoming State Pension Age Under Consideration" in {

      val expectedExclusion =
        TestAccountBuilder.getRawJson(TestAccountBuilder.spaUnderConsiderationExclusionIoMNino, "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionIoMNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionIoMNino, delegationState = false)) { result =>
        result shouldBe Right(Left(OkStatePensionExclusion(
          List(
            Exclusion.IsleOfMan
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        )))
      }
    }

    "return the state pension age flag as true for a user with MWRRE exclusion with a true flag for incoming State Pension Age Under Consideration" in {
      val expectedExclusion =
        TestAccountBuilder.getRawJson(TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino, "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino, delegationState = false)) { result =>
        result shouldBe Right(Left(OkStatePensionExclusion(
          List(
            Exclusion.MarriedWomenReducedRateElection
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        )))
      }
    }

    "return the state pension age flag as true for a user with Over SPA exclusion with a true flag for incoming State Pension Age Under Consideration" in {
      val expectedExclusion =
        TestAccountBuilder.getRawJson(TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino, "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino, delegationState = false)) { result =>
        result shouldBe Right(Left(OkStatePensionExclusion(
          List(
            Exclusion.PostStatePensionAge
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        )))
      }
    }

    "return the state pension age flag as true for a user with Multiple exclusions with a true flag for incoming State Pension Age Under Consideration" in {
      val expectedExclusion =
        TestAccountBuilder.getRawJson(TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino, "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino, delegationState = false)) { result =>
        result shouldBe Right(Left(OkStatePensionExclusion(
          List(
            Exclusion.PostStatePensionAge,
            Exclusion.AmountDissonance,
            Exclusion.MarriedWomenReducedRateElection,
            Exclusion.IsleOfMan
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        )))
      }
    }

    "return the state pension age flag as none for a user with exclusion with no flag for incoming State Pension Age Under Consideration" in {
      val expectedExclusion =
        TestAccountBuilder.getRawJson(TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino, "state-pension")

      server.stubFor(
        get(urlEqualTo(s"/ni/${TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino}"))
          .willReturn(ok(Json.toJson(expectedExclusion).toString()))
      )

      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino, delegationState = false)) { result =>
        result shouldBe Right(Left(OkStatePensionExclusion(
          List(
            Exclusion.IsleOfMan
          ),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = None
        )))
      }
    }
  }
}
