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

package uk.gov.hmrc.nisp.controllers

import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{status => getStatus, _}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.http.test.WireMockSupport

import java.lang.System.currentTimeMillis
import java.time.LocalDateTime
import java.util.UUID

class TermsConditionsControllerISpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with WireMockSupport
  with ScalaFutures
  with BeforeAndAfterEach {

  val uuid: UUID = UUID.randomUUID()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port" -> wireMockServer.port(),
      "microservice.services.identity-verification.port" -> wireMockServer.port()
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()

    wireMockServer.stubFor(post(urlEqualTo("/auth/authorise")).willReturn(ok(
      s"""{
         |"nino": "AA123456A",
         |"confidenceLevel": 200,
         |"loginTimes": {
         |  "currentLogin": "${LocalDateTime.now}",
         |  "previousLogin": "${LocalDateTime.now}"
         |  }
         |}
      """.stripMargin)))
  }

  "show" should {
    "return a 200 with the terms and conditions page when showBackLink isn't included" in {
      val request = FakeRequest("GET", s"/check-your-state-pension/terms-and-conditions")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString
        )

      val result = route(app, request)

      result map getStatus shouldBe Some(OK)
    }

    "return a 200 with the terms and conditions page when showBackLink is true" in {
      val request = FakeRequest("GET", s"/check-your-state-pension/terms-and-conditions?showBackLink=true")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString
        )

      val result = route(app, request)

      result map getStatus shouldBe Some(OK)
    }
  }
}
