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

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.Nino

import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HttpGet, HttpResponse, Upstream4xxResponse }

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
    TestAccountBuilder.noQualifyingYears,
    TestAccountBuilder.backendNotFound,
    TestAccountBuilder.spaUnderConsiderationNino,
    TestAccountBuilder.spaUnderConsiderationNoFlagNino,
    TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino,
    TestAccountBuilder.spaUnderConsiderationExclusionIoMNino,
    TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino,
    TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino,
    TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino,
    TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino,

    TestAccountBuilder.excludedAll,
    TestAccountBuilder.excludedAllButDead,
    TestAccountBuilder.excludedAllButDeadMCI,
    TestAccountBuilder.excludedDissonanceIomMwrreAbroad,
    TestAccountBuilder.excludedIomMwrreAbroad,
    TestAccountBuilder.excludedMwrreAbroad,
    TestAccountBuilder.excludedMwrre,
    TestAccountBuilder.excludedAbroad)

  val noDataNinos = List(
    TestAccountBuilder.backendNotFound
  )
  val ninosWithData = ninos.diff(noDataNinos)

  def createMockedURL(urlEndsWith: String, response: Future[HttpResponse]): Unit =
    when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(urlEndsWith))(ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn(response)

  def createFailedMockedURL(urlEndsWith: String): Unit =
    when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(urlEndsWith))(ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn(Future.failed(new BadRequestException("")))

  def setupNispEndpoints(nino: Nino): Unit = {
    createMockedURL(s"nisp/$nino/spsummary", TestAccountBuilder.jsonResponse(nino, "summary"))
    createMockedURL(s"nisp/$nino/nirecord", TestAccountBuilder.jsonResponse(nino, "nirecord"))
    createMockedURL(s"nisp/$nino/schememembership", TestAccountBuilder.jsonResponse(nino, "schememembership"))
  }

  def setupStatePensionEndpoints(nino: Nino): Unit = {
    createMockedURL(s"state-pension/ni/$nino", TestAccountBuilder.jsonResponse(nino, "state-pension"))
  }

  def setupNationalInsuranceEndpoints(nino: Nino): Unit = {
    createMockedURL(s"national-insurance/ni/$nino", TestAccountBuilder.jsonResponse(nino, "national-insurance-record"))
  }

  // NISP

  ninosWithData.foreach(setupNispEndpoints)

  val badRequestNino = TestAccountBuilder.nonExistentNino

  createFailedMockedURL(s"ni/$badRequestNino")

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"ni/${TestAccountBuilder.backendNotFound}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = """GET of 'http://url' returned 404. Response body: '{"code":"NOT_FOUND","message":"Resource was not found"}'""",
      upstreamResponseCode = 404,
      reportAs = 500
    )))

  // State Pension
  val spNinos = List(
    TestAccountBuilder.regularNino,
    TestAccountBuilder.spaUnderConsiderationNino,
    TestAccountBuilder.spaUnderConsiderationNoFlagNino,
    TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino,
    TestAccountBuilder.spaUnderConsiderationExclusionIoMNino,
    TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino,
    TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino,
    TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino,
    TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino,
    TestAccountBuilder.excludedAllButDeadMCI,
    TestAccountBuilder.excludedMwrre,
    TestAccountBuilder.excludedAbroad,
    TestAccountBuilder.fullUserNino,
    TestAccountBuilder.hrpNino

  )

  spNinos.foreach(setupStatePensionEndpoints)

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"ni/${TestAccountBuilder.excludedAll}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_DEAD\",\"message\":\"The customer needs to contact the National Insurance helpline\"}'",
      upstreamResponseCode = 403,
      reportAs = 500
    )))

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"ni/${TestAccountBuilder.excludedAllButDead}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_MANUAL_CORRESPONDENCE\",\"message\":\"TThe customer cannot access the service, they should contact HMRC\"}'",
      upstreamResponseCode = 403,
      reportAs = 500
    )))

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"ni/${TestAccountBuilder.backendNotFound}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = """GET of 'http://url' returned 404. Response body: '{"code":"NOT_FOUND","message":"Resource was not found"}'""",
      upstreamResponseCode = 404,
      reportAs = 500
    )))

  // National Insurance

  val niNinos = List(
    TestAccountBuilder.regularNino,
    TestAccountBuilder.fullUserNino,
    TestAccountBuilder.hrpNino

  )

  niNinos.foreach(setupNationalInsuranceEndpoints)

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"national-insurance/ni/${TestAccountBuilder.excludedAll}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_DEAD\",\"message\":\"The customer needs to contact the National Insurance helpline\"}'",
      upstreamResponseCode = 403,
      reportAs = 500
    )))

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"national-insurance/ni/${TestAccountBuilder.excludedAllButDead}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_MANUAL_CORRESPONDENCE\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
      upstreamResponseCode = 403,
      reportAs = 500
    )))

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"national-insurance/ni/${TestAccountBuilder.excludedAllButDeadMCI}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_ISLE_OF_MAN\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
      upstreamResponseCode = 403,
      reportAs = 500
    )))


  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"national-insurance/ni/${TestAccountBuilder.excludedIomMwrreAbroad}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_MARRIED_WOMENS_REDUCED_RATE\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
      upstreamResponseCode = 403,
      reportAs = 500
    )))

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"national-insurance/ni/${TestAccountBuilder.backendNotFound}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = """GET of 'http://url' returned 404. Response body: '{"code":"NOT_FOUND","message":"Resource was not found"}'""",
      upstreamResponseCode = 404,
      reportAs = 500
    )))

}
