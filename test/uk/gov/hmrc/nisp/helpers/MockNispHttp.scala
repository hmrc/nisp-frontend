/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.helpers
import uk.gov.hmrc.play.http.{BadRequestException, HttpGet, HttpResponse, Upstream4xxResponse}

import scala.concurrent.Future

object MockNispHttp extends MockitoSugar {
  val mockHttp = mock[HttpGet]
  val ninos = List(
    TestAccountBuilder.regularNino,
    TestAccountBuilder.blankNino,
    TestAccountBuilder.fullUserNino,
    TestAccountBuilder.contractedOutBTestNino,
    TestAccountBuilder.mqpNino,
    TestAccountBuilder.forecastOnlyNino,
    TestAccountBuilder.invalidKeyNino,
    TestAccountBuilder.abroadNino,
    TestAccountBuilder.mqpAbroadNino,
    TestAccountBuilder.hrpNino,
    TestAccountBuilder.fillGapsMultiple,
    TestAccountBuilder.fillGapSingle,

    TestAccountBuilder.excludedAll,
    TestAccountBuilder.excludedAllButDead,
    TestAccountBuilder.excludedAllButDeadMCI,
    TestAccountBuilder.excludedDissonanceIomMwrreAbroad,
    TestAccountBuilder.excludedIomMwrreAbroad,
    TestAccountBuilder.excludedMwrreAbroad,
    TestAccountBuilder.excludedAbroad)

  def createMockedURL(urlEndsWith: String, response: Future[HttpResponse]): Unit =
    when(mockHttp.GET[HttpResponse](Matchers.endsWith(urlEndsWith))(Matchers.any(), Matchers.any())).thenReturn(response)

  def createFailedMockedURL(urlEndsWith: String): Unit =
    when(mockHttp.GET[HttpResponse](Matchers.endsWith(urlEndsWith))(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new BadRequestException("")))

  def setupNispEndpoints(nino: Nino): Unit = {
    createMockedURL(s"nisp/$nino/spsummary", TestAccountBuilder.jsonResponse(nino, "summary"))
    createMockedURL(s"nisp/$nino/nirecord", TestAccountBuilder.jsonResponse(nino, "nirecord"))
    createMockedURL(s"nisp/$nino/schememembership", TestAccountBuilder.jsonResponse(nino, "schememembership"))
  }

  def setupStatePensionEndpoints(nino: Nino): Unit = {
    createMockedURL(s"ni/$nino", TestAccountBuilder.jsonResponse(nino, "state-pension"))
  }

  // NISP

  ninos.foreach(setupNispEndpoints)

  val badRequestNino = TestAccountBuilder.nonExistentNino

  createFailedMockedURL(s"nisp/$badRequestNino/spsummary")
  createFailedMockedURL(s"nisp/$badRequestNino/nirecord")

  // State Pension

  val spNinos = List(
    TestAccountBuilder.regularNino,
    TestAccountBuilder.excludedAllButDeadMCI
  )

  spNinos.foreach(setupStatePensionEndpoints)

  when(mockHttp.GET[HttpResponse](Matchers.endsWith(s"ni/${TestAccountBuilder.excludedAll}"))(Matchers.any(), Matchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_DEAD\",\"message\":\"The customer needs to contact the National Insurance helpline\"}'",
      upstreamResponseCode = 403,
      reportAs = 500
    )))

  when(mockHttp.GET[HttpResponse](Matchers.endsWith(s"ni/${TestAccountBuilder.excludedAllButDead}"))(Matchers.any(), Matchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_MANUAL_CORRESPONDENCE\",\"message\":\"TThe customer cannot access the service, they should contact HMRC\"}'",
      upstreamResponseCode = 403,
      reportAs = 500
    )))
}
