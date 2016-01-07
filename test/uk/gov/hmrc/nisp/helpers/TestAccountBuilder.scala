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

package uk.gov.hmrc.nisp.helpers

import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.http.HttpResponse

import scala.io.Source
import scala.util.Random

object TestAccountBuilder {
  def randomNino: String = new Generator(new Random()).nextNino.nino.replaceFirst("MA", "AA")

  val nonExistentNino: String = randomNino
  val excludedNino: String = randomNino
  val regularNino: String = randomNino
  val mqpNino: String = randomNino
  val contractedOutBTestNino: String = randomNino.replaceAll("[02468]", "1")
  val fullUserNino: String = randomNino
  val blankNino: String = randomNino
  val notFoundNino: String = randomNino
  val invalidKeyNino: String = randomNino
  val cachedNino: String = randomNino
  val noNameNino: String = randomNino

  val mappedTestAccounts = Map(
    excludedNino -> "excluded",
    regularNino -> "regular",
    mqpNino -> "mqp",
    contractedOutBTestNino -> "contractedout",
    fullUserNino ->  "fulluser",
    blankNino ->  "blank",
    invalidKeyNino -> "invalidkey",
    noNameNino -> "noname"
  )

  def jsonResponse(nino: String, api: String): HttpResponse = {
    val jsonFile = fileContents(s"test/resources/${mappedTestAccounts(nino)}/$api.json")
    HttpResponse(Status.OK, Some(Json.parse(jsonFile.replace("<NINO>", nino))))
  }

  private def fileContents(filename: String): String = Source.fromFile(filename).mkString
}
