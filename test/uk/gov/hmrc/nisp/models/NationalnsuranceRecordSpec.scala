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

package uk.gov.hmrc.nisp.models

import java.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.nisp.utils.UnitSpec


class NationalnsuranceRecordSpec extends UnitSpec {

  "NationalInsuranceRecord" when {
    "there are no years" should {
      "parse the json correctly" in {
        Json.parse(
          """
            |{
            |  "_links": {
            |    "self": {
            |      "href": "/national-insurance-record/ni/<NINO>"
            |    }
            |  },
            |  "qualifyingYears": 0,
            |  "qualifyingYearsPriorTo1975": 0,
            |  "numberOfGaps": 0,
            |  "numberOfGapsPayable": 0,
            |  "dateOfEntry": "2015-05-04",
            |  "homeResponsibilitiesProtection": false,
            |  "earningsIncludedUpTo": "2016-04-05",
            |  "_embedded": {
            |    "taxYears": []
            |  },
            |  "reducedRateElection": false
            |}
          """.stripMargin).as[NationalInsuranceRecord] shouldBe
          NationalInsuranceRecord(
            qualifyingYears = 0,
            qualifyingYearsPriorTo1975 = 0,
            numberOfGaps = 0,
            numberOfGapsPayable = 0,
            dateOfEntry = Some(LocalDate.of(2015, 5, 4)),
            homeResponsibilitiesProtection = false,
            earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
            List(),
            reducedRateElection = false
          )
      }
    }

    "there is no date of entry" should {
      "parse the json correctly" in {
        Json.parse(
          """
            |{
            |  "_links": {
            |    "self": {
            |      "href": "/national-insurance-record/ni/<NINO>"
            |    }
            |  },
            |  "qualifyingYears": 0,
            |  "qualifyingYearsPriorTo1975": 0,
            |  "numberOfGaps": 0,
            |  "numberOfGapsPayable": 0,
            |  "homeResponsibilitiesProtection": false,
            |  "earningsIncludedUpTo": "2016-04-05",
            |  "_embedded": {
            |    "taxYears": []
            |  },
            |  "reducedRateElection": false
            |}
          """.stripMargin).as[NationalInsuranceRecord] shouldBe
          NationalInsuranceRecord(
            qualifyingYears = 0,
            qualifyingYearsPriorTo1975 = 0,
            numberOfGaps = 0,
            numberOfGapsPayable = 0,
            dateOfEntry = None,
            homeResponsibilitiesProtection = false,
            earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
            List(),
            reducedRateElection = false
          )
      }
    }

    "there is only one year" should {
      "parse the json correctly" in {
        Json.parse(
          """
            |{
            |  "_links": {
            |    "self": {
            |      "href": "/national-insurance-record/ni/<NINO>"
            |    }
            |  },
            |  "qualifyingYears": 1,
            |  "qualifyingYearsPriorTo1975": 0,
            |  "numberOfGaps": 0,
            |  "numberOfGapsPayable": 0,
            |  "dateOfEntry": "2015-05-04",
            |  "homeResponsibilitiesProtection": false,
            |  "earningsIncludedUpTo": "2017-04-05",
            |  "_embedded": {
            |    "taxYears": {
            |      "_links": {
            |        "self": {
            |          "href": "/national-insurance-record/ni/<NINO>/taxyear/2016-17"
            |        }
            |      },
            |      "taxYear": "2016-17",
            |      "qualifying": true,
            |      "classOneContributions": 0,
            |      "classTwoCredits": 0,
            |      "classThreeCredits": 0,
            |      "otherCredits": 52,
            |      "classThreePayable": 0,
            |      "classThreePayableBy": null,
            |      "classThreePayableByPenalty": null,
            |      "payable": false,
            |      "underInvestigation": false
            |    }
            |  },
            |  "reducedRateElection": false
            |}
          """.stripMargin).as[NationalInsuranceRecord] shouldBe
          NationalInsuranceRecord(
            qualifyingYears = 1,
            qualifyingYearsPriorTo1975 = 0,
            numberOfGaps = 0,
            numberOfGapsPayable = 0,
            dateOfEntry = Some(LocalDate.of(2015, 5, 4)),
            homeResponsibilitiesProtection = false,
            earningsIncludedUpTo = LocalDate.of(2017, 4, 5),
            List(NationalInsuranceTaxYear(
              "2016-17",
              true,
              0,
              0,
              0,
              52,
              0,
              None,
              None,
              false,
              false
            )),
            reducedRateElection = false
          )
      }
    }

    "there is multiple years" should {
      "parse the json correctly" in {
        Json.parse(
          """
            |{
            |  "_links": {
            |    "self": {
            |      "href": "/national-insurance-record/ni/<NINO>"
            |    }
            |  },
            |  "qualifyingYears": 1,
            |  "qualifyingYearsPriorTo1975": 0,
            |  "numberOfGaps": 1,
            |  "numberOfGapsPayable": 1,
            |  "dateOfEntry": "2015-05-04",
            |  "homeResponsibilitiesProtection": false,
            |  "earningsIncludedUpTo": "2018-04-05",
            |  "reducedRateElection" : false,
            |  "_embedded": {
            |    "taxYears": [
            |      {
            |        "_links": {
            |          "self": {
            |            "href": "/national-insurance-record/ni/<NINO>/taxyear/2017-18"
            |          }
            |        },
            |        "taxYear": "2017-18",
            |        "qualifying": false,
            |        "classOneContributions": 0,
            |        "classTwoCredits": 0,
            |        "classThreeCredits": 0,
            |        "otherCredits": 0,
            |        "classThreePayable": 722.8,
            |        "classThreePayableBy": "2019-04-05",
            |        "classThreePayableByPenalty": "2023-04-05",
            |        "payable": true,
            |        "underInvestigation": false
            |      },
            |      {
            |        "_links": {
            |          "self": {
            |            "href": "/national-insurance-record/ni/<NINO>/taxyear/2016-17"
            |          }
            |        },
            |        "taxYear": "2016-17",
            |        "qualifying": true,
            |        "classOneContributions": 0,
            |        "classTwoCredits": 0,
            |        "classThreeCredits": 0,
            |        "otherCredits": 52,
            |        "classThreePayable": 0,
            |        "classThreePayableBy": null,
            |        "classThreePayableByPenalty": null,
            |        "payable": false,
            |        "underInvestigation": false
            |      }
            |    ]
            |  }
            |}
          """.stripMargin).as[NationalInsuranceRecord] shouldBe
          NationalInsuranceRecord(
            qualifyingYears = 1,
            qualifyingYearsPriorTo1975 = 0,
            numberOfGaps = 1,
            numberOfGapsPayable = 1,
            dateOfEntry = Some(LocalDate.of(2015, 5, 4)),
            homeResponsibilitiesProtection = false,
            earningsIncludedUpTo = LocalDate.of(2018, 4, 5),
            List(
              NationalInsuranceTaxYear(
                "2017-18",
                false,
                0,
                0,
                0,
                0,
                722.8,
                Some(LocalDate.of(2019, 4, 5)),
                Some(LocalDate.of(2023, 4, 5)),
                true,
                false
              ),
              NationalInsuranceTaxYear(
                "2016-17",
                true,
                0,
                0,
                0,
                52,
                0,
                None,
                None,
                false,
                false
              )
            ),
            reducedRateElection = false
          )
      }
    }
  }

}
