/*
 * Copyright 2015 HM Revenue & Customs
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

import org.joda.time.{LocalDateTime, LocalDate}
import uk.gov.hmrc.play.test.UnitSpec

class NpsAvailabilityCheckerSpec extends UnitSpec {

  def testNpsAvailabilityChecker(localDateTime: LocalDateTime) = new NpsAvailabilityChecker {
    override def now: LocalDateTime = localDateTime
  }

  def localDateTime(hours: Int, minutes: Int, seconds: Int): LocalDateTime = new LocalDateTime(2000,1,1,hours,minutes,seconds)


  "isNPSAvailable" when {
    "The time is 2:00:00" should {
      "return false" in {
        testNpsAvailabilityChecker(localDateTime(2,0,0)).isNPSAvailable shouldBe false
      }
    }

    "The time is 1:59:59" should {
      "return true" in {
        testNpsAvailabilityChecker(localDateTime(1,59,59)).isNPSAvailable shouldBe true
      }
    }

    "The time is 5:00:00" should {
      "return true" in {
        testNpsAvailabilityChecker(localDateTime(5,0,0)).isNPSAvailable shouldBe true
      }
    }

    "The time is 4:59:59" should {
      "return false" in {
        testNpsAvailabilityChecker(localDateTime(4,59,59)).isNPSAvailable shouldBe false
      }
    }
  }

}
