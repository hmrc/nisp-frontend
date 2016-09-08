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

package uk.gov.hmrc.nisp.helpers

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.http.{BadRequestException, HttpGet, HttpResponse}

import scala.concurrent.Future

object MockNispHttp extends MockitoSugar {
  val mockHttp = mock[HttpGet]
  val ninos = List(
    TestAccountBuilder.regularNino,
    TestAccountBuilder.blankNino,
    TestAccountBuilder.excludedNino,
    TestAccountBuilder.fullUserNino,
    TestAccountBuilder.contractedOutBTestNino,
    TestAccountBuilder.mqpNino,
    TestAccountBuilder.forecastOnlyNino,
    TestAccountBuilder.invalidKeyNino,
    TestAccountBuilder.abroadNino,
    TestAccountBuilder.mqpAbroadNino,
    TestAccountBuilder.hrpNino,
    TestAccountBuilder.fillGapsMultiple,
    TestAccountBuilder.fillGapSingle)

  val badRequestNino = TestAccountBuilder.nonExistentNino

  def createMockedURL(urlEndsWith: String, response: HttpResponse): Unit =
    when(mockHttp.GET[HttpResponse](Matchers.endsWith(urlEndsWith))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(response))

  def createFailedMockedURL(urlEndsWith: String): Unit =
    when(mockHttp.GET[HttpResponse](Matchers.endsWith(urlEndsWith))(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new BadRequestException("")))

  def setupNinoEndpoints(nino: String): Unit = {
    createMockedURL(s"nisp/$nino/spsummary", TestAccountBuilder.jsonResponse(nino, "summary"))
    createMockedURL(s"nisp/$nino/nirecord", TestAccountBuilder.jsonResponse(nino, "nirecord"))
    createMockedURL(s"nisp/$nino/schememembership", TestAccountBuilder.jsonResponse(nino, "schememembership"))
  }

  ninos.foreach(setupNinoEndpoints)

  createFailedMockedURL(s"nisp/$badRequestNino/spsummary")
  createFailedMockedURL(s"nisp/$badRequestNino/nirecord")
}
