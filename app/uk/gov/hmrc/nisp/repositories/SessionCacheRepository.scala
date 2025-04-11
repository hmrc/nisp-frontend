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

import com.google.inject.{ImplementedBy, Inject}
import play.api.Configuration
import play.api.libs.json.Format
import play.api.mvc.Request
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.cache.{DataKey, SessionCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.nisp.repositories.SessionCache.CacheKey

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[NispSessionCacheRepository])
trait SessionCache {
  def get[T](key: CacheKey[T])(implicit format: Format[T], request: Request[?]): Future[Option[T]]
  def put[T](key: CacheKey[T], value: T)(implicit format: Format[T], request: Request[?]): Future[Unit]
}

object SessionCache {
  sealed abstract class CacheKey[T]{val dataKey: DataKey[T]}
  object CacheKey {
    val PERTAX:CacheKey[Boolean]= new CacheKey[Boolean](){override val dataKey: DataKey[Boolean] = DataKey[Boolean]("customerPERTAX")}
  }
}

class NispSessionCacheRepository @Inject() (
  mongoComponent: MongoComponent,
  config: Configuration,
  timestampSupport: TimestampSupport
)(implicit ec: ExecutionContext)
    extends SessionCacheRepository(
      mongoComponent = mongoComponent,
      collectionName = "session-cache",
      ttl = Duration(config.get[Int]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS),
      timestampSupport = timestampSupport,
      sessionIdKey = SessionKeys.sessionId
    ) with SessionCache {

  override def get[T](key: CacheKey[T])(implicit format: Format[T], request: Request[?]): Future[Option[T]] =
    getFromSession(key.dataKey)

  override def put[T](key: CacheKey[T], value: T)(implicit format: Format[T], request: Request[?]): Future[Unit] =
    putSession(key.dataKey, value).map(_ => ())
}
