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

package uk.gov.hmrc.nisp.services

import org.mockito.ArgumentMatchers.{any => mockAny, eq => mockEQ}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.nisp.connectors.StatePensionConnector
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder._
import uk.gov.hmrc.nisp.models.{Exclusion, _}
import uk.gov.hmrc.nisp.utils.UnitSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StatePensionServiceSpec extends UnitSpec with ScalaFutures with BeforeAndAfterEach with PrivateMethodTester {

  implicit val defaultPatience  =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  val mockStatePensionConnector = mock[StatePensionConnector]

  val statePensionService = new StatePensionService(mockStatePensionConnector)(global) {
    override def now: () => LocalDate = () => LocalDate.of(2016, 11, 1)
  }

  def statePensionResponse[A](nino: Nino)(implicit fjs: Reads[A]): A = jsonResponseByType(nino, "state-pension")

  "yearsToContributeUntilPensionAge" should {
    "shouldBe 2 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2016-4-5" in {
      statePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 2
    }

    "shouldBe 3 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2015-4-5" in {
      statePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = LocalDate.of(2015, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 3
    }

    "shouldBe 1 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2017-4-5" in {
      statePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = LocalDate.of(2017, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 1
    }

    "shouldBe 0 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2018-4-5" in {
      statePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = LocalDate.of(2018, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 0
    }

    "shouldBe 0 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2017-4-6" in {
      statePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = LocalDate.of(2017, 4, 6),
        finalRelevantYearStart = 2017
      ) shouldBe 0
    }
  }

  "StatePensionConnection" should {

    implicit val headerCarrier = HeaderCarrier(extraHeaders = Seq("Accept" -> "application/vnd.hmrc.1.0+json"))

    "transform the Dead 403 into a Left(StatePensionExclusion(Dead))" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(excludedAll))(mockAny())).thenReturn(
        Future.failed(
          new Upstream4xxResponse(
            message =
              "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_DEAD\",\"message\":\"The customer needs to contact the National Insurance helpline\"}'",
            upstreamResponseCode = 403,
            reportAs = 500
          )
        )
      )

      whenReady(statePensionService.getSummary(excludedAll)) { exclusion =>
        exclusion shouldBe Left(StatePensionExclusionFiltered(Exclusion.Dead))
      }
    }

    "transform the MCI 403 into a Left(StatePensionExclusion(MCI))" in {

      when(mockStatePensionConnector.getStatePension(mockEQ(excludedAllButDead))(mockAny())).thenReturn(
        Future.failed(
          new Upstream4xxResponse(
            message =
              "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_MANUAL_CORRESPONDENCE\",\"message\":\"TThe customer cannot access the service, they should contact HMRC\"}'",
            upstreamResponseCode = 403,
            reportAs = 500
          )
        )
      )

      whenReady(statePensionService.getSummary(excludedAllButDead)) { exclusion =>
        exclusion shouldBe Left(StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator))
      }
    }

    "transform the COPE Failed 403 into a CopeProcessingFailed exclusion" in {
      val copeResponseProcessingFailed: String =
        "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_COPE_PROCESSING_FAILED\"}'"

      when(mockStatePensionConnector.getStatePension(mockEQ(regularNino))(mockAny())).thenReturn(
        Future.failed(
          new Upstream4xxResponse(message = copeResponseProcessingFailed, upstreamResponseCode = 403, reportAs = 500)
        )
      )

      whenReady(statePensionService.getSummary(regularNino)) { exclusion =>
        exclusion shouldBe Left(StatePensionExclusionFiltered(Exclusion.CopeProcessingFailed))
      }
    }

    "transform the COPE Failed 403 into a CopeProcessing exclusion" in {
      val copeResponseProcessing: String =
        "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_COPE_PROCESSING\",\"copeDataAvailableDate\":\"2021-02-17\"}'"

      when(mockStatePensionConnector.getStatePension(mockEQ(regularNino))(mockAny())).thenReturn(
        Future.failed(
          new Upstream4xxResponse(message = copeResponseProcessing, upstreamResponseCode = 403, reportAs = 500)
        )
      )

      whenReady(statePensionService.getSummary(regularNino)) { exclusion =>
        exclusion shouldBe Left(
          StatePensionExclusionFilteredWithCopeDate(
            Exclusion.CopeProcessing,
            copeAvailableDate = LocalDate.of(2021, 2, 17)
          )
        )
      }
    }

    "return the connector response for a regular user" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(regularNino))(mockAny())).thenReturn(
        Future.successful(Right(statePensionResponse[StatePension](regularNino)))
      )

      whenReady(statePensionService.getSummary(regularNino)) { statePension =>
        statePension shouldBe Right(
          StatePension(
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
            "2017-18",
            30,
            false,
            155.65,
            false,
            false
          )
        )
      }
    }

    "return the connector response for a RRE user" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(excludedMwrre))(mockAny())).thenReturn(
        Future.successful(Right(statePensionResponse[StatePension](excludedMwrre)))
      )

      whenReady(statePensionService.getSummary(excludedMwrre)) { statePension =>
        statePension shouldBe Right(
          StatePension(
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
            "2017-18",
            30,
            false,
            155.65,
            true,
            false
          )
        )
      }
    }

    "return the connector response for a Abroad user" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(excludedAbroad))(mockAny())).thenReturn(
        Future.successful(Right(statePensionResponse[StatePension](excludedAbroad)))
      )

      whenReady(statePensionService.getSummary(excludedAbroad)) { statePension =>
        statePension shouldBe Right(
          StatePension(
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
            "2017-18",
            30,
            false,
            155.65,
            false,
            false
          )
        )
      }
    }

    "return the connector response with PostStatePensionAge exclusion for all the exclusions except MCI and Dead" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(excludedAllButDeadMCI))(mockAny())).thenReturn(
        Future.successful(Left(statePensionResponse[StatePensionExclusion](excludedAllButDeadMCI)))
      )

      whenReady(statePensionService.getSummary(excludedAllButDeadMCI)) { statePension =>
        statePension shouldBe Left(
          StatePensionExclusionFiltered(
            Exclusion.PostStatePensionAge,
            pensionAge = Some(65),
            pensionDate = Some(LocalDate.of(2017, 7, 18)),
            statePensionAgeUnderConsideration = Some(false)
          )
        )
      }
    }

    "return the connector response for a user with a true flag for State Pension Age Under Consideration" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(spaUnderConsiderationNino))(mockAny())).thenReturn(
        Future.successful(Right(statePensionResponse[StatePension](spaUnderConsiderationNino)))
      )

      whenReady(statePensionService.getSummary(spaUnderConsiderationNino)) { statePension =>
        statePension shouldBe Right(
          StatePension(
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
            "2017-18",
            30,
            false,
            155.65,
            false,
            true
          )
        )
      }
    }

    "return the connector response for a user with no flag for State Pension Age Under Consideration" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(spaUnderConsiderationNoFlagNino))(mockAny())).thenReturn(
        Future.successful(Right(statePensionResponse[StatePension](spaUnderConsiderationNoFlagNino)))
      )

      whenReady(statePensionService.getSummary(spaUnderConsiderationNoFlagNino)) { statePension =>
        statePension shouldBe Right(
          StatePension(
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
            "2017-18",
            30,
            false,
            155.65,
            false,
            false
          )
        )
      }
    }

    "return the connector response for a user with exclusion with a true flag for State Pension Age Under Consideration" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(spaUnderConsiderationExclusionIoMNino))(mockAny()))
        .thenReturn(
          Future.successful(Left(statePensionResponse[StatePensionExclusion](spaUnderConsiderationExclusionIoMNino)))
        )

      whenReady(statePensionService.getSummary(spaUnderConsiderationExclusionIoMNino)) { statePension =>
        statePension shouldBe Left(
          StatePensionExclusionFiltered(
            Exclusion.IsleOfMan,
            pensionAge = Some(65),
            pensionDate = Some(LocalDate.of(2017, 7, 18)),
            statePensionAgeUnderConsideration = Some(true)
          )
        )
      }
    }

    "return the connector response for a user with exclusion with no flag for State Pension Age Under Consideration" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(spaUnderConsiderationExclusionNoFlagNino))(mockAny()))
        .thenReturn(
          Future.successful(Left(statePensionResponse[StatePensionExclusion](spaUnderConsiderationExclusionNoFlagNino)))
        )

      whenReady(statePensionService.getSummary(spaUnderConsiderationExclusionNoFlagNino)) { statePension =>
        statePension shouldBe Left(
          StatePensionExclusionFiltered(
            Exclusion.IsleOfMan,
            pensionAge = Some(65),
            pensionDate = Some(LocalDate.of(2017, 7, 18)),
            statePensionAgeUnderConsideration = None
          )
        )
      }
    }

    "Private method getDateWithRegex should return an exception when COPE response without date is given" in {
      val copeResponseProcessing: String =
        "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_COPE_PROCESSING\",\"copeDataAvailableDate\":\"aaaaaaa\"}'"

      when(mockStatePensionConnector.getStatePension(mockEQ(regularNino))(mockAny())).thenReturn(
        Future.failed(
          new Upstream4xxResponse(message = copeResponseProcessing, upstreamResponseCode = 403, reportAs = 500)
        )
      )

      val thrown = intercept[Exception](await(statePensionService.getSummary(regularNino)))

      assert(thrown.getMessage === "COPE date not matched with regex!")
    }

  }

}
