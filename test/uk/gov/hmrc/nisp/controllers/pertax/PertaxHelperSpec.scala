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

package uk.gov.hmrc.nisp.controllers.pertax

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.repositories.SessionCache
import uk.gov.hmrc.nisp.repositories.SessionCache.CacheKey.PERTAX
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.UnitSpec

import scala.concurrent.Future

class PertaxHelperSpec
  extends UnitSpec
    with GuiceOneAppPerSuite
    with Injecting
    with BeforeAndAfterEach {

  implicit val request: FakeRequest[_] = FakeRequest()

  implicit val headerCarrier: HeaderCarrier =
    HeaderCarrier()
  val mockSessionCache: SessionCache =
    mock[SessionCache]
  val mockMetricsService: MetricsService =
    mock[MetricsService](RETURNS_DEEP_STUBS)

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MetricsService].toInstance(mockMetricsService),
      bind[SessionCache].toInstance(mockSessionCache),
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetricsService.keystoreWriteTimer)
    reset(mockMetricsService.keystoreWriteFailed)
    reset(mockMetricsService.keystoreReadTimer)
    reset(mockMetricsService.keystoreHitCounter)
    reset(mockMetricsService.keystoreMissCounter)
    reset(mockSessionCache)
  }

  val pertaxHelper: PertaxHelper =
    inject[PertaxHelper]

  "PertaxHelperSpec.setFromPertax" should {
    "call timerContext.stop() when set value in cache succeeds" in {

      when(mockSessionCache.put(any(), any())(any(), any()))
        .thenReturn(Future.successful(()))

      await(pertaxHelper.setFromPertax) shouldBe ((): Unit)
      eventually{
        verify(mockSessionCache, times(1)).put(ArgumentMatchers.eq(PERTAX),any())(any(),any())
        verify(mockMetricsService.keystoreWriteTimer.time()).stop()
      }
    }
  }

  "PertaxHelperSpec.isFromPertax" should {
    "call timerContext.stop() and keystoreHitCounter.inc() when true returned from cache" in {
      when(mockSessionCache.get[Boolean](any())(any(), any()))
        .thenReturn(Future.successful(Some(true)))

      await(pertaxHelper.isFromPertax) shouldBe true

      eventually {
        verify(mockSessionCache, times(1)).get(ArgumentMatchers.eq(PERTAX))(any(),any())
        verify(mockMetricsService.keystoreReadTimer.time()).stop()
        verify(mockMetricsService.keystoreHitCounter).inc()
      }
    }

    "call timerContext.stop() and keystoreHitCounter.inc() when false returned from cache" in {
      when(mockSessionCache.get[Boolean](any())(any(), any()))
        .thenReturn(Future.successful(Some(false)))

      await(pertaxHelper.isFromPertax) shouldBe false

      eventually {
        verify(mockSessionCache, times(1)).get(ArgumentMatchers.eq(PERTAX))(any(),any())
        verify(mockMetricsService.keystoreReadTimer.time()).stop()
        verify(mockMetricsService.keystoreHitCounter).inc()
      }
    }

    "call timerContext.stop() and keystoreMissCounter.inc() when None returned from cache" in {
      when(mockSessionCache.get[Boolean](any())(any(), any()))
        .thenReturn(Future.successful(None))

      await(pertaxHelper.isFromPertax) shouldBe false

      eventually {
        verify(mockSessionCache, times(1)).get(ArgumentMatchers.eq(PERTAX))(any(),any())
        verify(mockMetricsService.keystoreReadTimer.time()).stop()
        verify(mockMetricsService.keystoreMissCounter).inc()
      }
    }

    "call keystoreReadFailed.inc() when fetch fails" in {
      when(mockSessionCache.get[Boolean](any())(any(), any()))
        .thenReturn(Future.failed(new Exception("this is an error")))

      await(pertaxHelper.isFromPertax) shouldBe false

      eventually {
        verify(mockMetricsService.keystoreReadFailed).inc()
      }
    }
  }
}
