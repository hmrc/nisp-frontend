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

package uk.gov.hmrc.nisp.connectors

import org.joda.time.LocalDate
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.nisp.connectors.NispConnector.JsonValidationException
import uk.gov.hmrc.nisp.helpers.{MockNispConnector, TestAccountBuilder}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.Scenario
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier, UserId}
import uk.gov.hmrc.play.test.UnitSpec

class NispConnectorSpec extends UnitSpec with MockitoSugar with  BeforeAndAfter with OneAppPerSuite with ScalaFutures {
  val nino = TestAccountBuilder.regularNino
  val errorNino = TestAccountBuilder.nonExistentNino
  val invalidKeyNino = TestAccountBuilder.invalidKeyNino
  val blankNino = TestAccountBuilder.blankNino
  val cachedNinoAndUsername = TestAccountBuilder.cachedNino
  val cachedUserId = UserId(s"/auth/oid/$cachedNinoAndUsername")

  def spSummary: SPSummaryModel = SPSummaryModel(
    TestAccountBuilder.regularNino.nino,
    NpsDate(2015,4,5),
    SPAmountModel(133.41,580.1,6961.14),
    SPAgeModel(64, NpsDate(2018,7,6)),
    finalRelevantYear = 2017,
    numberOfQualifyingYears = 30,
    numberOfGaps = 6,
    numberOfGapsPayable = 6,
    yearsToContributeUntilPensionAge = 30,
    hasPsod = false,
    dateOfBirth = NpsDate(1953,3,31),
    SPForecastModel(SPAmountModel(146.76,638.14,7657.73), 3, SPAmountModel(155.65,676.8,8121.59), 2, Scenario.FillGaps, false),
    fullNewStatePensionAmount = 155.65,
    contractedOutFlag = false,
    customerAge = 63,
    copeAmount = SPAmountModel(0,0,0),
    mqp = None,
    isAbroad = false
  )


  def niResponse:NIResponse = NIResponse(Some(NIRecord(List(
    NIRecordTaxYear(1976,true,0,0,0,52,None,None,None,false, false), NIRecordTaxYear(1977,true,0,0,0,52,None,None,None,false, false), NIRecordTaxYear(1978,true,325,0,0,0,None,None,None,false, false),
    NIRecordTaxYear(1979,true,390,0,0,0,None,None,None,false, false), NIRecordTaxYear(1980,true,472.5,0,0,0,None,None,None,false, false), NIRecordTaxYear(1981,true,620,0,0,0,None,None,None,false, false), NIRecordTaxYear(1982,true,787.5,0,0,0,None,None,None,false, false),
    NIRecordTaxYear(1983,true,900,0,0,0,None,None,None,false, false), NIRecordTaxYear(1984,true,1333.8,0,0,0,None,None,None,false, false), NIRecordTaxYear(1985,false,0,0,0,0,None,None,None,false, false), NIRecordTaxYear(1986,true,0,0,0,52,None,None,None,false, false),
    NIRecordTaxYear(1987,false,0,0,0,0,None,None,None,false, false), NIRecordTaxYear(1988,false,0,0,0,0,None,None,None,false, false), NIRecordTaxYear(1989,true,0,0,0,52,None,None,None,false, false), NIRecordTaxYear(1990,true,0,0,0,52,None,None,None,false, false),
    NIRecordTaxYear(1991,false,20.69,0,0,0,None,None,None,false, false), NIRecordTaxYear(1992,false,8.28,0,0,2,None,None,None,false, false), NIRecordTaxYear(1993,true,447.91,0,0,0,None,None,None,false, false), NIRecordTaxYear(1994,true,938.61,0,0,0,None,None,None,false, false),
    NIRecordTaxYear(1995,true,1046.32,0,0,0,None,None,None,false, false), NIRecordTaxYear(1996,true,1147.98,0,0,0,None,None,None,false, false), NIRecordTaxYear(1997,true,1296.18,0,0,0,None,None,None,false, false), NIRecordTaxYear(1998,true,1565.76,0,0,0,None,None,None,false, false),
    NIRecordTaxYear(1999,true,1763.98,0,0,0,None,None,None,false, false), NIRecordTaxYear(2000,true,1913.39,0,0,0,None,None,None,false, false), NIRecordTaxYear(2001,true,2011.61,0,0,0,None,None,None,false, false), NIRecordTaxYear(2002,true,2166.7,0,0,0,None,None,None,false, false),
    NIRecordTaxYear(2003,true,2484.49,0,0,0,None,None,None,false, false), NIRecordTaxYear(2004,true,2573.95,0,0,0,None,None,None,false, false), NIRecordTaxYear(2005,true,2495.74,0,0,0,None,None,None,false, false), NIRecordTaxYear(2006,false,0,0,0,0,None,None,None,false, false),
    NIRecordTaxYear(2007,true,3312,0,0,0,None,None,None,false, false), NIRecordTaxYear(2008,true,2699.4,0,0,0,None,None,None,false, false), NIRecordTaxYear(2009,true,4259.6,0,0,0,None,None,None,false, false), NIRecordTaxYear(2010,false,0,0,0,0,Some(626.6),Some(NpsDate(new LocalDate(2019,4,5))),Some(NpsDate(new LocalDate(2023,4,5))),true, false),
    NIRecordTaxYear(2011,false,0,0,0,0,Some(655.2),Some(NpsDate(new LocalDate(2019,4,5))),Some(NpsDate(new LocalDate(2023,4,5))),true, false), NIRecordTaxYear(2012,false,0,0,0,0,Some(689.0),Some(NpsDate(new LocalDate(2019,4,5))),Some(NpsDate(new LocalDate(2023,4,5))),true, false), NIRecordTaxYear(2013,false,0,0,0,0,Some(704.6),Some(NpsDate(new LocalDate(2019,4,5))),Some(NpsDate(new LocalDate(2023,4,5))),true, false)
  ))), Some(NISummary(28,10,13,2027,NpsDate(new LocalDate(2014,4,5)),2014,None,4,6,false, false, None, 2043, false)), None)

  "SPResponse" should {
    "return SPResponse instance with a SPSummary for valid JSON" in {
      MockNispConnector.connectToGetSPResponse(nino)(new HeaderCarrier()).spSummary.isEmpty shouldBe false
    }

    "return SPSummary instance with correct data" in {
      MockNispConnector.connectToGetSPResponse(nino)(new HeaderCarrier()).spSummary shouldBe Some(spSummary)
    }

    "return JsonValidationException for invalid JSON key" in {
      ScalaFutures.whenReady(MockNispConnector.connectToGetSPResponse(invalidKeyNino)(new HeaderCarrier()).failed) { e =>
        e shouldBe a [JsonValidationException]
      }
    }

    "return None for blank response" in {
      MockNispConnector.connectToGetSPResponse(blankNino)(new HeaderCarrier()).spSummary.isEmpty shouldBe true
      MockNispConnector.connectToGetSPResponse(blankNino)(new HeaderCarrier()).spExclusions.isEmpty shouldBe true
    }

    "return BadRequestException for BadRequest result from microservice" in {
      ScalaFutures.whenReady(MockNispConnector.connectToGetSPResponse(errorNino)(new HeaderCarrier()).failed){ e =>
        e shouldBe a [BadRequestException]
      }
    }

    "return cached SPResponse if existing" in {
      MockNispConnector.connectToGetSPResponse(cachedNinoAndUsername)(new HeaderCarrier(userId = Some(cachedUserId))).spSummary
        .map(_.copy(nino = TestAccountBuilder.regularNino.nino)) shouldBe Some(spSummary)
    }
  }

  "NI Response" should {
    "return NIResponse instance with correct data" in {
      MockNispConnector.connectToGetNIResponse(nino)(new HeaderCarrier()).niRecord shouldBe niResponse.niRecord
      MockNispConnector.connectToGetNIResponse(nino)(new HeaderCarrier()).niSummary shouldBe niResponse.niSummary
    }

    "return JsonValidationException for invalid JSON key in NI Record" in {
      ScalaFutures.whenReady(MockNispConnector.connectToGetNIResponse(invalidKeyNino)(new HeaderCarrier()).failed) { e =>
        e shouldBe a [JsonValidationException]
      }
    }

    "return a BadRequestException for BadRequest result from microservice in NI Record" in {
      ScalaFutures.whenReady(MockNispConnector.connectToGetNIResponse(errorNino)(new HeaderCarrier()).failed){ e =>
        e shouldBe a [BadRequestException]
      }
    }

    "return cached NIResponse if existing" in {
      MockNispConnector.connectToGetNIResponse(cachedNinoAndUsername)(new HeaderCarrier(userId = Some(cachedUserId))).niRecord shouldBe niResponse.niRecord
      MockNispConnector.connectToGetNIResponse(cachedNinoAndUsername)(new HeaderCarrier(userId = Some(cachedUserId))).niSummary shouldBe niResponse.niSummary
    }

    "return None for blank response" in {
      MockNispConnector.connectToGetNIResponse(blankNino)(new HeaderCarrier()).niRecord.isEmpty shouldBe true
      MockNispConnector.connectToGetNIResponse(blankNino)(new HeaderCarrier()).niSummary.isEmpty shouldBe true
    }
  }
}
