/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HttpClient, HttpResponse, Upstream4xxResponse}
import scala.concurrent.Future

object MockNispHttp extends MockitoSugar {
  val mockHttp: HttpClient = mock[HttpClient]

  val noDataNinos = List(
    TestAccountBuilder.backendNotFound
  )

  def createMockedURL(urlEndsWith: String, response: Future[HttpResponse]): Unit =
    when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(urlEndsWith))(ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn(response)

  def createFailedMockedURL(urlEndsWith: String): Unit =
    when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(urlEndsWith))(ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn(Future.failed(new BadRequestException("")))

  def setupStatePensionEndpoints(nino: Nino): Unit = {
    createMockedURL(s"state-pension/ni/$nino", TestAccountBuilder.jsonResponse(nino, "state-pension"))
  }

  def setupNationalInsuranceEndpoints(nino: Nino): Unit = {
    createMockedURL(s"national-insurance/ni/$nino", TestAccountBuilder.jsonResponse(nino, "national-insurance-record"))
  }

  val badRequestNino = TestAccountBuilder.nonExistentNino

  createFailedMockedURL(s"ni/$badRequestNino")

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"ni/${TestAccountBuilder.backendNotFound}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = """GET of 'http://url' returned 404. Response body: '{"code":"NOT_FOUND","message":"Resource was not found"}'""",
      upstreamResponseCode = 404,
      reportAs = 500
    )))

  val spNinos = List(
    TestAccountBuilder.regularNino,
    TestAccountBuilder.urBannerNino,
    TestAccountBuilder.noUrBannerNino,
    TestAccountBuilder.fullUserNino,
    TestAccountBuilder.forecastOnlyNino,
    TestAccountBuilder.fillGapSingle,
    TestAccountBuilder.fillGapsMultiple,
    TestAccountBuilder.contractedOutBTestNino,
    TestAccountBuilder.hrpNino,
    TestAccountBuilder.noQualifyingYears,
    TestAccountBuilder.abroadNino,
    TestAccountBuilder.mqpNino,
    TestAccountBuilder.mqpAbroadNino,
    TestAccountBuilder.excludedAll,
    TestAccountBuilder.excludedAllButDead,
    TestAccountBuilder.excludedAllButDeadMCI,
    TestAccountBuilder.excludedAbroad,
    TestAccountBuilder.excludedMwrre,
    TestAccountBuilder.excludedMwrreAbroad,
    TestAccountBuilder.excludedIomMwrreAbroad,
    TestAccountBuilder.excludedDissonanceIomMwrreAbroad,
    TestAccountBuilder.spaUnderConsiderationNino,
    TestAccountBuilder.spaUnderConsiderationNoFlagNino,
    TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino,
    TestAccountBuilder.spaUnderConsiderationExclusionIoMNino,
    TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino,
    TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino,
    TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino,
    TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino
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

  val niNinos = List(
    TestAccountBuilder.regularNino,
    TestAccountBuilder.fullUserNino,
    TestAccountBuilder.forecastOnlyNino,
    TestAccountBuilder.fillGapSingle,
    TestAccountBuilder.fillGapsMultiple,
    TestAccountBuilder.contractedOutBTestNino,
    TestAccountBuilder.hrpNino,
    TestAccountBuilder.noQualifyingYears,
    TestAccountBuilder.abroadNino,
    TestAccountBuilder.mqpNino,
    TestAccountBuilder.mqpAbroadNino,
    TestAccountBuilder.excludedAbroad,
    TestAccountBuilder.urBannerNino,
    TestAccountBuilder.noUrBannerNino,
    TestAccountBuilder.excludedDissonanceIomMwrreAbroad,
    TestAccountBuilder.spaUnderConsiderationNino,
    TestAccountBuilder.spaUnderConsiderationNoFlagNino,
    TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino,
    TestAccountBuilder.spaUnderConsiderationExclusionIoMNino,
    TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino,
    TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino,
    TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino,
    TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino
  )

  niNinos.foreach(setupNationalInsuranceEndpoints)

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"national-insurance/ni/${TestAccountBuilder.excludedMwrreAbroad}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_MARRIED_WOMENS_REDUCED_RATE\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
      upstreamResponseCode = 403,
      reportAs = 500
    )))

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"national-insurance/ni/${TestAccountBuilder.excludedDissonanceIomMwrreAbroad}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_ISLE_OF_MAN\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
      upstreamResponseCode = 403,
      reportAs = 500
    )))

  when(mockHttp.GET[HttpResponse](ArgumentMatchers.endsWith(s"national-insurance/ni/${TestAccountBuilder.excludedMwrre}"))
    (ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(Future.failed(new Upstream4xxResponse(
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_MARRIED_WOMENS_REDUCED_RATE\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
      upstreamResponseCode = 403,
      reportAs = 500
    )))

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
      message = "GET of 'http://url' returned 403. Response body: '{\"code\":[\"EXCLUSION_MARRIED_WOMENS_REDUCED_RATE\",\"EXCLUSION_ISLE_OF_MAN\"],\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
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
