/*
 * Copyright 2019 HM Revenue & Customs
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

import org.joda.time.LocalDate
import uk.gov.hmrc.nisp.models.{NpsDate, NIRecordTaxYear}
import uk.gov.hmrc.play.test.UnitSpec

class NIRecordTaxYearSpec extends UnitSpec  {
  "NIRecordTaxYear cutOffDate" should {
    "return true if [payable gaps by] date is after the current date" in {
      val fakeNIRecordTaxYear = NIRecordTaxYear(2014,false,0,0,0,0,None,Some(NpsDate(2019,4,5)),Some(NpsDate(2023,4,5)),true,false)
      val testCutOffDate = fakeNIRecordTaxYear.checkCutOffDate(Some(NpsDate(2019,4,5)),new LocalDate(2016,8,25))
      testCutOffDate shouldBe true
    }

     "return false if current date is after [payable gaps by] date" in {
      val fakeNIRecordTaxYear = NIRecordTaxYear(2014,false,0,0,0,0,None,Some(NpsDate(2019,4,5)),Some(NpsDate(2023,4,5)),true,false)
      val testCutOffDate = fakeNIRecordTaxYear.checkCutOffDate(Some(NpsDate(2019,4,5)),new LocalDate(2019,8,25))
      testCutOffDate shouldBe false
    }

     "return false if current date is the same with [payable gaps by] date" in {
      val fakeNIRecordTaxYear = NIRecordTaxYear(2014,false,0,0,0,0,None,Some(NpsDate(2019,4,5)),Some(NpsDate(2023,4,5)),true,false)
      val testCutOffDate = fakeNIRecordTaxYear.checkCutOffDate(Some(NpsDate(2019,4,5)),new LocalDate(2019,4,5))
      testCutOffDate shouldBe false
    }

  }
}
