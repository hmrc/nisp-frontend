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

package uk.gov.hmrc.nisp.connectors

import org.joda.time.LocalDate
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import uk.gov.hmrc.nisp.helpers.{MockStatePensionConnector, TestAccountBuilder}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.test.UnitSpec

class StatePensionConnectorSpec extends UnitSpec with ScalaFutures {

  implicit val headerCarrier = HeaderCarrier(extraHeaders = Seq("Accept" -> "application/vnd.hmrc.1.0+json"))

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  "getStatePension" should {
    "return the correct response for the regular test user" in {
      whenReady(MockStatePensionConnector.getStatePension(TestAccountBuilder.regularNino)) { statePension =>
        statePension shouldBe Right(StatePension(
          new LocalDate(2015, 4, 5),
          StatePensionAmounts(
            false,
            StatePensionAmountRegular(133.41, 580.1, 6961.14),
            StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
            StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
            StatePensionAmountRegular(0, 0, 0)
          ),
          64, new LocalDate(2018, 7, 6), "2017-18", 30, false, 155.65
        ))
      }
    }

    "return a failed Future 403 with a Dead message for all exclusion" in {
      whenReady(MockStatePensionConnector.getStatePension(TestAccountBuilder.excludedAll).failed) {
        case ex: Upstream4xxResponse =>
          ex.upstreamResponseCode shouldBe 403
          ex.message.contains("EXCLUSION_DEAD") shouldBe true
      }
    }

    "return a failed Future 403 with a Dead message for all exclusion but dead" in {
      whenReady(MockStatePensionConnector.getStatePension(TestAccountBuilder.excludedAllButDead).failed) {
        case ex: Upstream4xxResponse =>
          ex.upstreamResponseCode shouldBe 403
          ex.message.contains("EXCLUSION_MANUAL_CORRESPONDENCE") shouldBe true
      }
    }

    "return the correct list of exclusions for the the user with all but dead and MCI" in {
      whenReady(MockStatePensionConnector.getStatePension(TestAccountBuilder.excludedAllButDeadMCI)) { result =>
        result shouldBe Left(StatePensionExclusion(
          List(
            Exclusion.PostStatePensionAge,
            Exclusion.AmountDissonance,
            Exclusion.MarriedWomenReducedRateElection,
            Exclusion.IsleOfMan,
            Exclusion.Abroad
          ),
          pensionAge = Some(65),
          pensionDate = Some(new LocalDate(2017, 7, 18))
        ))
      }
    }
  }

}
