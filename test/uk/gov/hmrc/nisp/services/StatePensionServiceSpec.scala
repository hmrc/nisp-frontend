/*
 * Copyright 2017 HM Revenue & Customs
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

import org.joda.time.LocalDate
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import uk.gov.hmrc.nisp.helpers.{MockStatePensionServiceViaNisp, MockStatePensionServiceViaStatePension, TestAccountBuilder}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class StatePensionServiceSpec extends UnitSpec with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  "yearsToContributeUntilPensionAge" should {
    "shouldBe 2 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2016-4-5" in {
      MockStatePensionServiceViaNisp.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 2
    }

    "shouldBe 3 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2015-4-5" in {
      MockStatePensionServiceViaNisp.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2015, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 3
    }

    "shouldBe 1 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2017-4-5" in {
      MockStatePensionServiceViaNisp.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2017, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 1
    }

    "shouldBe 0 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2018-4-5" in {
      MockStatePensionServiceViaNisp.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2018, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 0
    }

    "shouldBe 0 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2017-4-6" in {
      MockStatePensionServiceViaNisp.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2017, 4, 6),
        finalRelevantYearStart = 2017
      ) shouldBe 0
    }
  }

  "StatePensionConnection" should {

    implicit val headerCarrier = HeaderCarrier(extraHeaders = Seq("Accept" -> "application/vnd.hmrc.1.0+json"))

    "transform the Dead 403 into a Left(StatePensionExclusion(Dead))" in {
      whenReady(MockStatePensionServiceViaStatePension.getSummary(TestAccountBuilder.excludedAll)) { exclusion =>
        exclusion shouldBe Left(StatePensionExclusionFiltered(Exclusion.Dead))
      }
    }

    "transform the MCI 403 into a Left(StatePensionExclusion(MCI))" in {
      whenReady(MockStatePensionServiceViaStatePension.getSummary(TestAccountBuilder.excludedAllButDead)) { exclusion =>
        exclusion shouldBe Left(StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator))
      }
    }

    "return the connector response for a regular user" in {
      whenReady(MockStatePensionServiceViaStatePension.getSummary(TestAccountBuilder.regularNino)) { statePension =>
        statePension shouldBe Right(StatePension(
          new LocalDate(2015, 4, 5),
          StatePensionAmounts(
            false,
            StatePensionAmountRegular(133.41, 580.1, 6961.14),
            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
            StatePensionAmountRegular(0, 0, 0)
          ),
          64, new LocalDate(2018, 7, 6), "2017-18", 30, false, 155.65, false
        ))
      }
    }

    "return the connector response for a RRE user" in {
      whenReady(MockStatePensionServiceViaStatePension.getSummary(TestAccountBuilder.excludedMwrre)) { statePension =>
        statePension shouldBe Right(StatePension(
          new LocalDate(2015, 4, 5),
          StatePensionAmounts(
            false,
            StatePensionAmountRegular(133.41, 580.1, 6961.14),
            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
            StatePensionAmountRegular(0, 0, 0)
          ),
          64, new LocalDate(2018, 7, 6), "2017-18", 30, false, 155.65, true
        ))
      }
    }

    "return the connector response with PostStatePensionAge exclusion for all the exclusions except MCI and Dead" in {
      whenReady(MockStatePensionServiceViaStatePension.getSummary(TestAccountBuilder.excludedAllButDeadMCI)) { statePension =>
        statePension shouldBe Left(StatePensionExclusionFiltered(
          Exclusion.PostStatePensionAge,
          pensionAge = Some(65),
          pensionDate = Some(new LocalDate(2017, 7, 18))
        ))
      }
    }
  }

}
