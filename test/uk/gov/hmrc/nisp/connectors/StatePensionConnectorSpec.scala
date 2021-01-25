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

import org.joda.time.LocalDate
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, Upstream4xxResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.helpers.{MockNispHttp, TestAccountBuilder}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.play.test.UnitSpec

class StatePensionConnectorSpec extends UnitSpec with ScalaFutures with MockitoSugar with GuiceOneAppPerSuite with
  Injecting with BeforeAndAfterEach with MockNispHttp {

  implicit val headerCarrier = HeaderCarrier(extraHeaders = Seq("Accept" -> "application/vnd.hmrc.1.0+json"))

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val mockMetricService = mock[MetricsService]
  val mockApplicationConfig = mock[ApplicationConfig]
  val mockSessionCache = mock[SessionCache]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[HttpClient].toInstance(mockHttp),
      bind[MetricsService].toInstance(mockMetricService),
      bind[ApplicationConfig].toInstance(mockApplicationConfig),
      bind[SessionCache].toInstance(mockSessionCache)
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttp, mockMetricService, mockApplicationConfig, mockSessionCache)
  }

  val statePensionConnector = inject[StatePensionConnector]

  "getStatePension" should {
    "return the correct response for the regular test user" in {
      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.regularNino)) { statePension =>
        statePension shouldBe Right(StatePension(
          new LocalDate(2015, 4, 5),
          StatePensionAmounts(
            false,
            StatePensionAmountRegular(133.41, 580.1, 6961.14),
            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
            StatePensionAmountRegular(0, 0, 0)
          ),
          64, new LocalDate(2018, 7, 6), "2017-18", 30, false, 155.65,
          false,
          false
        ))
      }
    }

    "return a failed Future 403 with a Dead message for all exclusion" in {
      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.excludedAll).failed) {
        case ex: Upstream4xxResponse =>
          ex.upstreamResponseCode shouldBe 403
          ex.message.contains("EXCLUSION_DEAD") shouldBe true
      }
    }

    "return a failed Future 403 with a MCI message for all exclusion but dead" in {
      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.excludedAllButDead).failed) {
        case ex: Upstream4xxResponse =>
          ex.upstreamResponseCode shouldBe 403
          ex.message.contains("EXCLUSION_MANUAL_CORRESPONDENCE") shouldBe true
      }
    }

    "return the correct list of exclusions for the the user with all but dead and MCI" in {
      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.excludedAllButDeadMCI)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.PostStatePensionAge,
            Exclusion.AmountDissonance,
            Exclusion.MarriedWomenReducedRateElection,
            Exclusion.IsleOfMan
          ),
          pensionAge = Some(65),
          pensionDate = Some(new LocalDate(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(false)
        ))
      }
    }

    "return the state pension age flag as true for a user with Amount Dis exclusion with a true flag for incoming State Pension Age Under Consideration" in {
      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.AmountDissonance
          ),
          pensionAge = Some(65),
          pensionDate = Some(new LocalDate(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        ))
      }
    }

    "return the state pension age flag as true for a user with IOM exclusion with a true flag for incoming State Pension Age Under Consideration" in {
      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionIoMNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.IsleOfMan
          ),
          pensionAge = Some(65),
          pensionDate = Some(new LocalDate(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        ))
      }
    }

    "return the state pension age flag as true for a user with MWRRE exclusion with a true flag for incoming State Pension Age Under Consideration" in {
      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.MarriedWomenReducedRateElection
          ),
          pensionAge = Some(65),
          pensionDate = Some(new LocalDate(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        ))
      }
    }

    "return the state pension age flag as true for a user with Over SPA exclusion with a true flag for incoming State Pension Age Under Consideration" in {
      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.PostStatePensionAge
          ),
          pensionAge = Some(65),
          pensionDate = Some(new LocalDate(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        ))
      }
    }

    "return the state pension age flag as true for a user with Multiple exclusions with a true flag for incoming State Pension Age Under Consideration" in {
      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.PostStatePensionAge,
            Exclusion.AmountDissonance,
            Exclusion.MarriedWomenReducedRateElection,
            Exclusion.IsleOfMan
          ),
          pensionAge = Some(65),
          pensionDate = Some(new LocalDate(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
        ))
      }
    }

    "return the state pension age flag as none for a user with exclusion with no flag for incoming State Pension Age Under Consideration" in {
      whenReady(statePensionConnector.getStatePension(TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.IsleOfMan
          ),
          pensionAge = Some(65),
          pensionDate = Some(new LocalDate(2017, 7, 18)),
          statePensionAgeUnderConsideration = None
        ))
      }
    }
  }
}
