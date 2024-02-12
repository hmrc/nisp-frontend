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

package uk.gov.hmrc.nisp.services

import org.mockito.ArgumentMatchers.{any => mockAny, eq => mockEQ}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.connectors.StatePensionConnector
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder._
import uk.gov.hmrc.nisp.models.{Exclusion, _}
import uk.gov.hmrc.nisp.utils.UnitSpec
import java.time.LocalDate

import uk.gov.hmrc.nisp.models.StatePensionExclusion.{CopeStatePensionExclusion, ForbiddenStatePensionExclusion, OkStatePensionExclusion}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StatePensionServiceSpec extends UnitSpec with ScalaFutures with BeforeAndAfterEach with PrivateMethodTester {

  implicit val defaultPatience  =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))
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
        Future.successful(Right(Left(ForbiddenStatePensionExclusion(Exclusion.Dead, Some("The customer needs to contact the National Insurance helpline")))))
      )

      statePensionService.getSummary(excludedAll).futureValue shouldBe Right(Left(StatePensionExclusionFiltered(Exclusion.Dead)))
    }

    "transform the MCI 403 into a Left(StatePensionExclusion(MCI))" in {

      when(mockStatePensionConnector.getStatePension(mockEQ(excludedAllButDead))(mockAny())).thenReturn(
        Future.successful(Right(Left(ForbiddenStatePensionExclusion(Exclusion.ManualCorrespondenceIndicator, Some("The customer cannot access the service, they should contact HMRC")))))
      )

      statePensionService.getSummary(excludedAllButDead).futureValue shouldBe Right(Left(StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator)))
    }

    "transform the COPE Failed 403 into a CopeProcessingFailed exclusion" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(regularNino))(mockAny()))
        .thenReturn(Future.successful(
          Right(Left(ForbiddenStatePensionExclusion(Exclusion.CopeProcessingFailed, None)))))

      statePensionService.getSummary(regularNino).futureValue shouldBe Right(Left(StatePensionExclusionFiltered(Exclusion.CopeProcessingFailed)))
    }

    "transform the COPE Failed 403 into a CopeProcessing exclusion" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(regularNino))(mockAny()))
        .thenReturn(Future.successful(Right(Left(CopeStatePensionExclusion(Exclusion.CopeProcessing, LocalDate.of(2021, 2, 17), None)))))

      statePensionService.getSummary(regularNino).futureValue shouldBe
        Right(Left(StatePensionExclusionFilteredWithCopeDate(Exclusion.CopeProcessing, LocalDate.of(2021,2,17))))
    }

    "return the connector response for a regular user" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(statePensionResponse[StatePension](regularNino))))
      )

     statePensionService.getSummary(regularNino).futureValue shouldBe Right(Right(StatePension(
          LocalDate.of(2015, 4, 5),
          StatePensionAmounts(
            false,
            StatePensionAmountRegular(133.41, 580.1, 6961.14),
            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
            StatePensionAmountRegular(0, 0, 0)
          ),
          64, LocalDate.of(2018, 7, 6), "2017-18", 30, false, 155.65, false, false
      )))
    }

    "return the connector response for a RRE user" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(excludedMwrre))(mockAny())).thenReturn(
        Future.successful(Right(Right(statePensionResponse[StatePension](excludedMwrre))))
      )

      statePensionService.getSummary(excludedMwrre).futureValue shouldBe Right(Right(StatePension(
          LocalDate.of(2015, 4, 5),
          StatePensionAmounts(
            false,
            StatePensionAmountRegular(133.41, 580.1, 6961.14),
            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
            StatePensionAmountRegular(0, 0, 0)
          ),
          64, LocalDate.of(2018, 7, 6), "2017-18", 30, false, 155.65, true, false
      )))
    }

    "return the connector response for a Abroad user" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(excludedAbroad))(mockAny())).thenReturn(
        Future.successful(Right(Right(statePensionResponse[StatePension](excludedAbroad))))
      )

      statePensionService.getSummary(excludedAbroad).futureValue shouldBe Right(Right(StatePension(
          LocalDate.of(2015, 4, 5),
          StatePensionAmounts(
            false,
            StatePensionAmountRegular(133.41, 580.1, 6961.14),
            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
            StatePensionAmountRegular(0, 0, 0)
          ),
          64, LocalDate.of(2018, 7, 6), "2017-18", 30, false, 155.65, false, false
      )))
    }

    "return the connector response with PostStatePensionAge exclusion for all the exclusions except MCI and Dead" in {
      val spResponse = statePensionResponse[OkStatePensionExclusion](excludedAllButDeadMCI)

      when(mockStatePensionConnector.getStatePension(mockEQ(excludedAllButDeadMCI))(mockAny())).thenReturn(
        Future.successful(Right(Left(spResponse)))
      )

      statePensionService.getSummary(excludedAllButDeadMCI).futureValue shouldBe Right(Left(StatePensionExclusionFiltered(
          Exclusion.PostStatePensionAge,
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(false)
      )))
    }

    "return the connector response for a user with a true flag for State Pension Age Under Consideration" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(spaUnderConsiderationNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(statePensionResponse[StatePension](spaUnderConsiderationNino))))
      )

      statePensionService.getSummary(spaUnderConsiderationNino).futureValue shouldBe Right(Right(StatePension(
          LocalDate.of(2015, 4, 5),
          StatePensionAmounts(
            false,
            StatePensionAmountRegular(133.41, 580.1, 6961.14),
            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
            StatePensionAmountRegular(0, 0, 0)
          ),
          64, LocalDate.of(2018, 7, 6), "2017-18", 30, false, 155.65, false, true
      )))
    }

    "return the connector response for a user with no flag for State Pension Age Under Consideration" in {
      when(mockStatePensionConnector.getStatePension(mockEQ(spaUnderConsiderationNoFlagNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(statePensionResponse[StatePension](spaUnderConsiderationNoFlagNino))))
      )

      statePensionService.getSummary(spaUnderConsiderationNoFlagNino).futureValue shouldBe Right(Right(StatePension(
          LocalDate.of(2015, 4, 5),
          StatePensionAmounts(
            false,
            StatePensionAmountRegular(133.41, 580.1, 6961.14),
            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
            StatePensionAmountRegular(0, 0, 0)
          ),
          64, LocalDate.of(2018, 7, 6), "2017-18", 30, false, 155.65, false, false
      )))
    }

    "return the connector response for a user with exclusion with a true flag for State Pension Age Under Consideration" in {
      val spResponse = statePensionResponse[OkStatePensionExclusion](spaUnderConsiderationExclusionIoMNino)

      when(mockStatePensionConnector.getStatePension(mockEQ(spaUnderConsiderationExclusionIoMNino))(mockAny())).thenReturn(
        Future.successful(Right(Left(spResponse)))
      )

      statePensionService.getSummary(spaUnderConsiderationExclusionIoMNino).futureValue shouldBe Right(Left(StatePensionExclusionFiltered(
          Exclusion.IsleOfMan,
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = Some(true)
      )))
    }

    "return the connector response for a user with exclusion with no flag for State Pension Age Under Consideration" in {
      val spResponse = statePensionResponse[StatePensionExclusion](spaUnderConsiderationExclusionNoFlagNino)

      when(mockStatePensionConnector.getStatePension(mockEQ(spaUnderConsiderationExclusionNoFlagNino))(mockAny())).thenReturn(
        Future.successful(Right(Left(spResponse)))
      )

      statePensionService.getSummary(spaUnderConsiderationExclusionNoFlagNino).futureValue shouldBe Right(Left(StatePensionExclusionFiltered(
          Exclusion.IsleOfMan,
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.of(2017, 7, 18)),
          statePensionAgeUnderConsideration = None
      )))
    }

    "return StatePensionExclusionWithCopeDate when CopeStatePensionExclusion is given" in {
      val date = LocalDate.now()
      when(mockStatePensionConnector.getStatePension(mockEQ(regularNino))(mockAny())).thenReturn(Future.successful(Right(Left(CopeStatePensionExclusion(Exclusion.CopeProcessing, date, None)))))

      statePensionService.getSummary(regularNino).futureValue shouldBe Right(Left(StatePensionExclusionFilteredWithCopeDate(Exclusion.CopeProcessing, date)))
    }

  }

}
