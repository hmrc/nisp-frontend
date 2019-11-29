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

package uk.gov.hmrc.nisp.helpers

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, HttpGet, HttpResponse, NotFoundException, Upstream5xxResponse}

object MockCitizenDetailsHttp extends UnitSpec with MockitoSugar {
  val mockHttp = mock[HttpGet]
  val ninos = List(
    TestAccountBuilder.regularNino,
    TestAccountBuilder.blankNino,
    TestAccountBuilder.fullUserNino,
    TestAccountBuilder.contractedOutBTestNino,
    TestAccountBuilder.mqpNino,
    TestAccountBuilder.forecastOnlyNino,
    TestAccountBuilder.noNameNino,
    TestAccountBuilder.abroadNino,
    TestAccountBuilder.mqpAbroadNino,
    TestAccountBuilder.hrpNino,
    TestAccountBuilder.fillGapSingle,
    TestAccountBuilder.fillGapsMultiple,
    TestAccountBuilder.noQualifyingYears,
    TestAccountBuilder.backendNotFound,

    TestAccountBuilder.urBannerNino,
    TestAccountBuilder.noUrBannerNino,

    TestAccountBuilder.excludedAll,
    TestAccountBuilder.excludedAllButDead,
    TestAccountBuilder.excludedAllButDeadMCI,
    TestAccountBuilder.excludedDissonanceIomMwrreAbroad,
    TestAccountBuilder.excludedIomMwrreAbroad,
    TestAccountBuilder.excludedMwrreAbroad,
    TestAccountBuilder.excludedAbroad,
    TestAccountBuilder.excludedMwrre,

    TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino,
    TestAccountBuilder.spaUnderConsiderationExclusionIoMNino,
    TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino,
    TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino,
    TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino,
    TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino
  )
  def createMockedURL(nino: Nino, response: Future[HttpResponse]): Unit =
    when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"citizen-details/$nino/designatory-details"))(ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn(response)

  private def setupCitizenDetailsMocking(nino: Nino) = createMockedURL(nino, TestAccountBuilder.jsonResponse(nino, "citizen-details"))

  ninos.foreach(setupCitizenDetailsMocking)

  createMockedURL(TestAccountBuilder.notFoundNino, Future.failed(new NotFoundException("")))
  createMockedURL(TestAccountBuilder.nonExistentNino, Future.failed(new NotFoundException("")))
  createMockedURL(TestAccountBuilder.blankNino, Future.failed(new BadRequestException("")))
  createMockedURL(TestAccountBuilder.internalServerError, Future.failed(new Upstream5xxResponse("CRITICAL FAILURE", 500, 500)))
}
