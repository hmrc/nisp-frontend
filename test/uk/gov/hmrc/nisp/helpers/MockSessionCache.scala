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

import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

object MockSessionCache extends SessionCache{
  val cachedNinoAndUsername = TestAccountBuilder.cachedNino
  val cachedUserId = UserId(s"/auth/oid/$cachedNinoAndUsername")

  override def defaultSource: String = ???
  override def baseUri: String = ???
  override def domain: String = ???
  override def http: HttpGet with HttpPut with HttpDelete = ???

  private def loadObjectBasedOnKey[T](key: String)(implicit rds: Reads[T]): Option[T] =
    key match {
      case _ => None
    }

  override def fetchAndGetEntry[T](key: String)(implicit hc: HeaderCarrier, rds: Reads[T],ec:ExecutionContext): Future[Option[T]] =
    Future.successful(hc.userId.filter(_ == cachedUserId).flatMap(_ => loadObjectBasedOnKey(key)))

  override def cache[A](formId: String, body: A)(implicit wts: Writes[A], hc: HeaderCarrier,ec:ExecutionContext): Future[CacheMap] = Future.successful(CacheMap("", Map()))
}
