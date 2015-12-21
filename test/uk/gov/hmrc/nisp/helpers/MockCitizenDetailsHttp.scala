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

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.libs.json.{Json, JsValue}
import uk.gov.hmrc.play.http.{BadRequestException, NotFoundException, HttpResponse, HttpPost}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.io.Source

object MockCitizenDetailsHttp extends UnitSpec with MockitoSugar {
  val mockHttp = mock[HttpPost]
  val ninos = List(
    TestAccountBuilder.regularNino,
    TestAccountBuilder.blankNino,
    TestAccountBuilder.excludedNino,
    TestAccountBuilder.fullUserNino,
    TestAccountBuilder.contractedOutBTestNino,
    TestAccountBuilder.mqpNino,
    TestAccountBuilder.noNameNino
  )

  def createMockedURL(nino: String, response: Future[HttpResponse]): Unit =
    when(mockHttp.POST[JsValue, HttpResponse](Matchers.endsWith(s"citizen-details/$nino/designatory-details/summary"), Matchers.any(),
      Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(response)

  def responseFromSource(filename: String): HttpResponse = {
    val fileContents = Source.fromFile(filename).mkString
    if (fileContents.isEmpty)
      HttpResponse(Status.OK, responseString = Some(fileContents))
    else
      HttpResponse(Status.OK, Some(Json.parse(fileContents)))
  }

  private def setupCitizenDetailsMocking(nino: String) =
    createMockedURL(nino, TestAccountBuilder.jsonResponse(nino, "citizen-details"))

  ninos.foreach(setupCitizenDetailsMocking)

  createMockedURL(TestAccountBuilder.notFoundNino, Future.failed(new NotFoundException("")))
  createMockedURL(TestAccountBuilder.nonExistentNino, Future.failed(new NotFoundException("")))
  createMockedURL(TestAccountBuilder.blankNino, Future.failed(new BadRequestException("")))
}
