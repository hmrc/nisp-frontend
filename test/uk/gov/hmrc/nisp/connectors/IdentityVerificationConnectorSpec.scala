/*
 * Copyright 2024 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import org.mockito.Mockito
import org.mockito.Mockito.reset
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{FORBIDDEN, IM_A_TEAPOT, NOT_FOUND}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.{UnitSpec, WireMockHelper}

import scala.io.Source
import scala.util.Using

class IdentityVerificationConnectorSpec
    extends UnitSpec
    with GuiceOneAppPerSuite
    with ScalaFutures
    with Injecting
    with BeforeAndAfterEach
    with WireMockHelper {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  import IdentityVerificationSuccessResponse._

  val mockMetricService: MetricsService = mock[MetricsService](Mockito.RETURNS_DEEP_STUBS)
  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetricService)
    reset(mockApplicationConfig)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MetricsService].toInstance(mockMetricService)
    )
    .configure(
      "microservice.services.identity-verification.port" -> server.port()
    )
    .build()

  lazy val identityVerificationConnector: IdentityVerificationConnector = inject[IdentityVerificationConnector]

  val possibleJournies: Map[String, String] = Map(
    "success-journey-id"               -> "test/resources/identity-verification/success.json",
    "incomplete-journey-id"            -> "test/resources/identity-verification/incomplete.json",
    "failed-matching-journey-id"       -> "test/resources/identity-verification/failed-matching.json",
    "insufficient-evidence-journey-id" -> "test/resources/identity-verification/insufficient-evidence.json",
    "locked-out-journey-id"            -> "test/resources/identity-verification/locked-out.json",
    "user-aborted-journey-id"          -> "test/resources/identity-verification/user-aborted.json",
    "timeout-journey-id"               -> "test/resources/identity-verification/timeout.json",
    "technical-issue-journey-id"       -> "test/resources/identity-verification/technical-issue.json",
    "precondition-failed-journey-id"   -> "test/resources/identity-verification/precondition-failed.json",
    "invalid-journey-id"               -> "test/resources/identity-verification/invalid-result.json",
    "invalid-fields-journey-id"        -> "test/resources/identity-verification/invalid-fields.json",
    "failed-iv-journey-id"             -> "test/resources/identity-verification/failed-iv.json"
  )

  def mockJourneyId(journeyId: String): Unit = {
    val fileContents = Using(Source.fromFile(possibleJournies(journeyId))) {
      resource => resource.mkString
    }.get

    server.stubFor(
      get(urlEqualTo(s"/mdtp/journey/journeyId/$journeyId"))
        .willReturn(ok(Json.parse(fileContents).toString()))
    )
  }

  def identityVerificationResponse(journeyId: String, ivResponse: String): Assertion = {
    mockJourneyId(journeyId)
    await(
      identityVerificationConnector.identityVerificationResponse(journeyId)
    ) shouldBe IdentityVerificationSuccessResponse(ivResponse)
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

  "return an error when a status of NOT_FOUND is returned from downstream" in {
    server.stubFor(
      get(urlEqualTo(s"/mdtp/journey/journeyId/notFound"))
        .willReturn(aResponse().withStatus(NOT_FOUND)))

    await(identityVerificationConnector.identityVerificationResponse("notFound")) shouldBe IdentityVerificationNotFoundResponse
  }

  "return an error when a status of FORBIDDEN is returned from downstream" in {
    server.stubFor(
      get(urlEqualTo(s"/mdtp/journey/journeyId/forbidden"))
        .willReturn(aResponse().withStatus(FORBIDDEN)))

    await(identityVerificationConnector.identityVerificationResponse("forbidden")) shouldBe IdentityVerificationForbiddenResponse
  }

  "return an error when a status of anything else is returned from downstream" in {
    server.stubFor(
      get(urlEqualTo(s"/mdtp/journey/journeyId/teapot"))
        .willReturn(aResponse().withStatus(IM_A_TEAPOT)))

    await(identityVerificationConnector.identityVerificationResponse("teapot")) shouldBe a[IdentityVerificationErrorResponse]
  }

  "return failed future for invalid json fields" in {

    val journeyId = "invalid-fields-journey-id"
    mockJourneyId(journeyId)

    eventually {
      await(identityVerificationConnector.identityVerificationResponse(journeyId)) shouldBe a[IdentityVerificationErrorResponse]
    }
  }
}
