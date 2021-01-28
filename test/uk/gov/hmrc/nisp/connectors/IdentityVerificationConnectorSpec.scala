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

package uk.gov.hmrc.nisp.connectors

import org.mockito.Mockito.{reset, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.Future
import scala.io.Source

class IdentityVerificationConnectorSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite with ScalaFutures
  with Injecting with BeforeAndAfterEach {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  import IdentityVerificationSuccessResponse._

  val mockHttpClient = mock[HttpClient]
  val mockMetricService = mock[MetricsService](Mockito.RETURNS_DEEP_STUBS)
  val mockApplicationConfig = mock[ApplicationConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient, mockMetricService, mockApplicationConfig)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[HttpClient].toInstance(mockHttpClient),
      bind[MetricsService].toInstance(mockMetricService),
      bind[ApplicationConfig].toInstance(mockApplicationConfig)
    ).build()

  lazy val identityVerificationConnector = inject[IdentityVerificationConnector]

  val possibleJournies = Map(
    "success-journey-id" -> "test/resources/identity-verification/success.json",
    "incomplete-journey-id" -> "test/resources/identity-verification/incomplete.json",
    "failed-matching-journey-id" -> "test/resources/identity-verification/failed-matching.json",
    "insufficient-evidence-journey-id" -> "test/resources/identity-verification/insufficient-evidence.json",
    "locked-out-journey-id" -> "test/resources/identity-verification/locked-out.json",
    "user-aborted-journey-id" -> "test/resources/identity-verification/user-aborted.json",
    "timeout-journey-id" -> "test/resources/identity-verification/timeout.json",
    "technical-issue-journey-id" -> "test/resources/identity-verification/technical-issue.json",
    "precondition-failed-journey-id" -> "test/resources/identity-verification/precondition-failed.json",
    "invalid-journey-id" -> "test/resources/identity-verification/invalid-result.json",
    "invalid-fields-journey-id" -> "test/resources/identity-verification/invalid-fields.json",
    "failed-iv-journey-id" -> "test/resources/identity-verification/failed-iv.json"
  )

  def mockJourneyId(journeyId: String): Unit = {
    val fileContents = Source.fromFile(possibleJournies(journeyId)).mkString
    when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.contains(journeyId))(ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.any())).
      thenReturn(Future.successful(HttpResponse(Status.OK, Json.parse(fileContents).toString())))
  }

  def identityVerificationResponse(journeyId: String, ivResponse: String): Assertion = {
    mockJourneyId(journeyId)
    identityVerificationConnector.identityVerificationResponse(journeyId).futureValue shouldBe IdentityVerificationSuccessResponse(ivResponse)
  }

  "return success when identityVerification returns success" in {
    behave like identityVerificationResponse("success-journey-id", Success)
  }

  "return incomplete when identityVerification returns incomplete" in {
    behave like identityVerificationResponse("incomplete-journey-id", Incomplete)
  }

  "return failed matching when identityVerification returns failed matching" in {
    behave like identityVerificationResponse("failed-matching-journey-id", FailedMatching)
  }

  "return failed iv when identityVerification returns failed matching" in {
    behave like identityVerificationResponse("failed-iv-journey-id", FailedIV)
  }

  "return insufficient evidence when identityVerification returns insufficient evidence" in {
    behave like identityVerificationResponse("insufficient-evidence-journey-id", InsufficientEvidence)
  }

  "return locked out when identityVerification returns locked out" in {
    behave like identityVerificationResponse("locked-out-journey-id", LockedOut)
  }

  "return user aborted when identityVerification returns user aborted" in {
    behave like identityVerificationResponse("user-aborted-journey-id", UserAborted)
  }

  "return timeout when identityVerification returns timeout" in {
    behave like identityVerificationResponse("timeout-journey-id", Timeout)
  }

  "return technical issue when identityVerification returns technical issue" in {
    behave like identityVerificationResponse("technical-issue-journey-id", TechnicalIssue)
  }

  "return precondition failed when identityVerification returns precondition failed" in {
    behave like identityVerificationResponse("precondition-failed-journey-id", PreconditionFailed)
  }

  "return no failure when identityVerification returns non-existant result type" in {
    behave like identityVerificationResponse("invalid-journey-id", "ABCDEFG")
  }

  "return failed future for invalid json fields" in {
    val journeyId = "invalid-fields-journey-id"
    mockJourneyId(journeyId)
    val result = identityVerificationConnector.identityVerificationResponse(journeyId)
    ScalaFutures.whenReady(result) { e =>
      e shouldBe a [IdentityVerificationErrorResponse]
    }
  }
}