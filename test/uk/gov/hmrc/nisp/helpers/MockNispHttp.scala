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
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.{BadRequestException, NotFoundException, HttpResponse, HttpGet}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.io.Source

object MockNispHttp extends UnitSpec with MockitoSugar {
  val mockHttp = mock[HttpGet]
  val ninos = List(
    TestAccountBuilder.regularNino,
    TestAccountBuilder.blankNino,
    TestAccountBuilder.excludedNino,
    TestAccountBuilder.fullUserNino,
    TestAccountBuilder.contractedOutBTestNino,
    TestAccountBuilder.mqpNino,
    TestAccountBuilder.forecastOnlyNino,
    TestAccountBuilder.invalidKeyNino)

  val badRequestNino = TestAccountBuilder.nonExistentNino

  def createMockedURL(urlEndsWith: String, response: Future[HttpResponse]): Unit =
    when(mockHttp.GET[HttpResponse](Matchers.endsWith(urlEndsWith))(Matchers.any(), Matchers.any())).thenReturn(response)

  def createFailedMockedURL(urlEndsWith: String): Unit =
    when(mockHttp.GET[HttpResponse](Matchers.endsWith(urlEndsWith))(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new BadRequestException("")))

  def responseFromSource(filename: String): HttpResponse = {
    val fileContents = Source.fromFile(filename).mkString
    if (fileContents.isEmpty)
      HttpResponse(Status.OK, responseString = Some(fileContents))
    else
      HttpResponse(Status.OK, Some(Json.parse(fileContents)))
  }

  def setupNinoEndpoints(nino: String): Unit = {
    createMockedURL(s"nisp/$nino/spsummary", TestAccountBuilder.jsonResponse(nino, "summary"))
    createMockedURL(s"nisp/$nino/nirecord", TestAccountBuilder.jsonResponse(nino, "nirecord"))
  }

  ninos.foreach(setupNinoEndpoints)

  createFailedMockedURL(s"nisp/$badRequestNino/spsummary")
  createFailedMockedURL(s"nisp/$badRequestNino/nirecord")
}
