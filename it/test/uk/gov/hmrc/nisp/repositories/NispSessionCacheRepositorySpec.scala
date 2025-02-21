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

package uk.gov.hmrc.nisp.repositories

import org.scalatest.concurrent.Eventually.eventually
import play.api.Configuration
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.nisp.repositories.SessionCache.CacheKey.PERTAX
import uk.gov.hmrc.nisp.utils.UnitSpec

import java.util.UUID
import scala.concurrent.ExecutionContext

class NispSessionCacheRepositorySpec extends UnitSpec with MongoSupport {

  private val configuration = Configuration(
    "mongodb.timeToLiveInSeconds" -> "60",
  )

  private val sut: SessionCache = new NispSessionCacheRepository(
    mongoComponent,
    configuration,
    timestampSupport = new CurrentTimestampSupport()
  )(ExecutionContext.Implicits.global)

  "The NispSessionCacheRepository" must {

    "support cache operations" in {

      implicit val request: Request[?] = FakeRequest().withSession(SessionKeys.sessionId -> UUID.randomUUID().toString)

      val testValue = true

      eventually(await(sut.get(PERTAX)) shouldBe None)
      eventually(await(sut.put(PERTAX, testValue)) shouldBe ())
      eventually(await(sut.get(PERTAX)) shouldBe Some(testValue))
    }
  }

}
