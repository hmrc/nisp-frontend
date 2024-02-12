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

package uk.gov.hmrc.nisp.utils

class CountrySpec extends UnitSpec {
  "isAbroad" should {
    "should return true if the Country is not in the UK" in {
      Country.isAbroad("FRANCE") shouldBe true
    }

    "should return false if the Country is GREAT BRITAIN" in {
      Country.isAbroad("GREAT BRITAIN") shouldBe false
    }

    "should return false if the Country is ISLE OF MAN" in {
      Country.isAbroad("ISLE OF MAN") shouldBe false
    }

    "should return false if the Country is NORTHERN IRELAND" in {
      Country.isAbroad("NORTHERN IRELAND") shouldBe false
    }

    "should return false if the Country is ENGLAND" in {
      Country.isAbroad("ENGLAND") shouldBe false
    }

    "should return false if the Country is SCOTLAND" in {
      Country.isAbroad("SCOTLAND") shouldBe false
    }

    "should return false if the Country is WALES" in {
      Country.isAbroad("WALES") shouldBe false
    }

    "should return false if the Country is NOT SPECIFIED OR NOT USED" in {
      Country.isAbroad("NOT SPECIFIED OR NOT USED") shouldBe false
    }

  }
}
