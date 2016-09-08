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

package uk.gov.hmrc.nisp.models

import org.joda.time.LocalDate
import uk.gov.hmrc.play.test.UnitSpec

class SchemeMembershipSpec extends UnitSpec {

  "SchemeMembership formatting" should {
    "return April 2016 for SchemeEndDate (2016,4,5) " in {
      SchemeMembership(new LocalDate(2015, 4, 6), new LocalDate(2016, 4, 5)).endDateFormatted contains "April 2016"
      SchemeMembership(new LocalDate(2015, 4, 6), new LocalDate(2016, 1, 5)).endDateFormatted contains "January 2016"
      SchemeMembership(new LocalDate(2015, 4, 6), new LocalDate(2016, 12, 5)).endDateFormatted contains "December 2016"
    }
    "return April 1999 for SchemeStartDate (1999,4,5) " in {
      SchemeMembership(new LocalDate(1999, 4, 6), new LocalDate(2016, 4, 5)).startDateFormatted contains "April 1999"
      SchemeMembership(new LocalDate(2015, 11, 6), new LocalDate(2016, 4, 5)).startDateFormatted contains "November 2015"
      SchemeMembership(new LocalDate(2015, 6, 6), new LocalDate(2016, 4, 5)).startDateFormatted contains "June 2015"
    }
  }
}
