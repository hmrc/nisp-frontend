/*
 * Copyright 2016 HM Revenue & Customs
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
import uk.gov.hmrc.nisp.helpers.MockStatePensionService
import uk.gov.hmrc.play.test.UnitSpec

class StatePensionServiceSpec extends UnitSpec {

  "yearsToContributeUntilPensionAge" should {
    "shouldBe 2 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2016-4-5" in {
      MockStatePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 2
    }

    "shouldBe 3 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2015-4-5" in {
      MockStatePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2015, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 3
    }

    "shouldBe 1 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2017-4-5" in {
      MockStatePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2017, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 1
    }

    "shouldBe 0 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2018-4-5" in {
      MockStatePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2018, 4, 5),
        finalRelevantYearStart = 2017
      ) shouldBe 0
    }

    "shouldBe 0 when finalRelevantYear is 2017-18 and earningsIncludedUpTo is 2017-4-6" in {
      MockStatePensionService.yearsToContributeUntilPensionAge(
        earningsIncludedUpTo = new LocalDate(2017, 4, 6),
        finalRelevantYearStart = 2017
      ) shouldBe 0
    }
  }

}
