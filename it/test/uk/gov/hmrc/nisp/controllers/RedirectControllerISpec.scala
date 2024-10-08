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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.MOVED_PERMANENTLY
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{defaultAwaitTimeout, route, writeableOf_AnyContentAsEmpty, status => getStatus}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.http.test.WireMockSupport

import java.lang.System.currentTimeMillis
import java.util.UUID

class RedirectControllerISpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with WireMockSupport
  with ScalaFutures
  with BeforeAndAfterEach
  with Injecting {

  val uuid: UUID = UUID.randomUUID()

  wireMockServer.start()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port" -> wireMockServer.port(),
      "microservice.services.citizen-details.port" -> wireMockServer.port()
    )
    .build()


  "redirectToHome" should {
    "return a 301 and use the a blank path when a blank path is supplied" in {
      val request = FakeRequest("GET", s"/checkmystatepension")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
          SessionKeys.authToken -> "Bearer 123"
        )

      val result = route(app, request)
      result map getStatus shouldBe Some(MOVED_PERMANENTLY)
    }

    "return a 301 when a path is provided" in {
      val request = FakeRequest("GET", s"/checkmystatepension/newpath")
        .withSession(
          SessionKeys.sessionId -> s"session-$uuid",
          SessionKeys.lastRequestTimestamp -> currentTimeMillis().toString,
          SessionKeys.authToken -> "Bearer 123"
        )

      val result = route(app, request)
      result map getStatus shouldBe Some(MOVED_PERMANENTLY)
    }
  }
}
