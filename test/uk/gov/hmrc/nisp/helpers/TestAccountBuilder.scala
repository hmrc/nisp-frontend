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

package uk.gov.hmrc.nisp.helpers

import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.domain.{Generator, Nino}

import scala.concurrent.Future
import scala.io.Source
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.nisp.models.citizen.CitizenDetailsResponse

object TestAccountBuilder {

  def randomNino: Nino = Nino(new Generator(new Random()).nextNino.nino.replaceFirst("MA", "AA"))

  val nonExistentNino: Nino = randomNino
  val regularNino: Nino = randomNino
  val mqpNino: Nino = randomNino
  val forecastOnlyNino: Nino = randomNino
  val contractedOutBTestNino: Nino = Nino(randomNino.nino.replaceAll("[02468]", "1"))
  val fullUserNino: Nino = randomNino
  val blankNino: Nino = randomNino
  val notFoundNino: Nino = randomNino
  val spaUnderConsiderationNino: Nino = randomNino
  val spaUnderConsiderationNoFlagNino: Nino = randomNino
  val spaUnderConsiderationExclusionAmountDisNino: Nino = randomNino
  val spaUnderConsiderationExclusionIoMNino: Nino = randomNino
  val spaUnderConsiderationExclusionMwrreNino: Nino = randomNino
  val spaUnderConsiderationExclusionOverSpaNino: Nino = randomNino
  val spaUnderConsiderationExclusionMultipleNino: Nino = randomNino
  val spaUnderConsiderationExclusionNoFlagNino: Nino = randomNino

  val urBannerNino: Nino = randomNino
  val noUrBannerNino: Nino = randomNino

  val invalidKeyNino: Nino = randomNino
  val cachedNino: Nino = randomNino
  val noNameNino: Nino = randomNino
  val weakNino: Nino = randomNino
  val abroadNino: Nino = randomNino
  val mqpAbroadNino: Nino = randomNino
  val hrpNino: Nino = randomNino
  val fillGapSingle: Nino = randomNino
  val fillGapsMultiple: Nino = randomNino
  val noQualifyingYears: Nino = randomNino
  val backendNotFound: Nino = randomNino

  val excludedAll: Nino = randomNino
  val excludedAllButDead: Nino = randomNino
  val excludedAllButDeadMCI: Nino = randomNino
  val excludedDissonanceIomMwrreAbroad: Nino = randomNino
  val excludedIomMwrreAbroad: Nino = randomNino
  val excludedMwrreAbroad: Nino = randomNino

  val excludedMwrre: Nino = randomNino

  val excludedAbroad: Nino = randomNino

  val internalServerError: Nino = randomNino

  val mappedTestAccounts = Map(
    regularNino -> "regular",
    mqpNino -> "mqp",
    forecastOnlyNino -> "forecastonly",
    contractedOutBTestNino -> "contractedout",
    fullUserNino -> "fulluser",
    blankNino -> "blank",
    invalidKeyNino -> "invalidkey",
    noNameNino -> "noname",
    abroadNino -> "abroad",
    mqpAbroadNino -> "mqp_abroad",
    hrpNino -> "homeresponsibilitiesprotection",
    fillGapSingle -> "fillgaps-singlegap",
    fillGapsMultiple -> "fillgaps-multiple",
    noQualifyingYears -> "no-qualifying-years",
    backendNotFound -> "backend-not-found",
    spaUnderConsiderationNino -> "spa-under-consideration",
    spaUnderConsiderationNoFlagNino -> "spa-under-consideration-no-flag",
    spaUnderConsiderationExclusionAmountDisNino -> "spa-under-consideration-exclusion-amount-dis",
    spaUnderConsiderationExclusionIoMNino -> "spa-under-consideration-exclusion-iom",
    spaUnderConsiderationExclusionMwrreNino -> "spa-under-consideration-exclusion-mwrre",
    spaUnderConsiderationExclusionOverSpaNino -> "spa-under-consideration-exclusion-over-spa",
    spaUnderConsiderationExclusionMultipleNino -> "spa-under-consideration-exclusion-multiple",
    spaUnderConsiderationExclusionNoFlagNino -> "spa-under-consideration-exclusion-no-flag",

    urBannerNino -> "showurbanner",
    noUrBannerNino -> "hideurbanner",

    excludedAll -> "excluded-all",
    excludedAllButDead -> "excluded-all-but-dead",
    excludedAllButDeadMCI -> "excluded-all-but-dead-mci",
    excludedDissonanceIomMwrreAbroad -> "excluded-dissonance-iom-mwrre-abroad",
    excludedIomMwrreAbroad -> "excluded-iom-mwrre-abroad",
    excludedMwrreAbroad -> "excluded-mwrre-abroad",
    excludedMwrre -> "excluded-mwrre",
    excludedAbroad -> "excluded-abroad"
  )

  def directJsonResponse(nino: Nino, api: String): CitizenDetailsResponse = {
    val string  = Source.fromFile(s"test/resources/${mappedTestAccounts(nino)}/$api.json").mkString
    Json.parse(string.replace("<NINO>", nino.nino)).as[CitizenDetailsResponse]
  }

  private def fileContents(filename: String): Future[String] = Future { Source.fromFile(filename).mkString }

  def jsonResponse(nino: Nino, api: String): Future[HttpResponse] = {
    fileContents(s"test/resources/${mappedTestAccounts(nino)}/$api.json").map { string: String =>
      HttpResponse(Status.OK, Some(Json.parse(string.replace("<NINO>", nino.nino))))
    }
  }
}
