/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.libs.json.Json
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder
import uk.gov.hmrc.nisp.models.citizen.{Address, Citizen, CitizenDetailsResponse}
import uk.gov.hmrc.play.test.UnitSpec


class CitizenDetailsResponseSpec extends UnitSpec {
    "Citizen" should {

      val nino = TestAccountBuilder.regularNino
      val citizenDetailsResponse = CitizenDetailsResponse(
        Citizen(
          nino,
          Some("AHMED"),
          Some("BRENNAN"),
          new LocalDate(1954, 3, 9)
        ),
        Some(Address(
          country = Some("USA")
        ))
      )

      "parse correctly when date of birth is a date" in {
        Json.parse(
          s"""
            |{
            |  "person":{
            |    "sex":"M",
            |    "dateOfBirth":-499132800000,
            |    "nino":"$nino",
            |    "firstName":"AHMED",
            |    "middleName":"",
            |    "lastName":"BRENNAN",
            |    "title":"Mrs",
            |    "honours":null
            |  },
            |  "address":{
            |    "line1":"108 SAI ROAD",
            |    "line2":"",
            |    "line3":"",
            |    "line4":null,
            |    "postcode":"12345",
            |    "country":"USA",
            |    "startDate":1223510400000,
            |    "type":"Residential"
            |  }
            |}
          """.stripMargin
        ).as[CitizenDetailsResponse] shouldBe citizenDetailsResponse
      }

      "parse correctly when date of birth is a long" in {
        Json.parse(
          s"""
             |{
             |  "person":{
             |    "sex":"M",
             |    "dateOfBirth":"1954-03-09",
             |    "nino":"$nino",
             |    "firstName":"AHMED",
             |    "middleName":"",
             |    "lastName":"BRENNAN",
             |    "title":"Mrs",
             |    "honours":null
             |  },
             |  "address":{
             |    "line1":"108 SAI ROAD",
             |    "line2":"",
             |    "line3":"",
             |    "line4":null,
             |    "postcode":"12345",
             |    "country":"USA",
             |    "startDate":1223510400000,
             |    "type":"Residential"
             |  }
             |}
          """.stripMargin
        ).as[CitizenDetailsResponse] shouldBe citizenDetailsResponse
      }
    }
}
