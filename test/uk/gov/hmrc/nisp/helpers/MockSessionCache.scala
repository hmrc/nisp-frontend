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

import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import uk.gov.hmrc.http.{HeaderCarrier, HttpDelete, HttpGet, HttpPut, UserId}

object MockSessionCache extends SessionCache{
  val cachedNinoAndUsername = TestAccountBuilder.cachedNino
  val cachedUserId = UserId(s"/auth/oid/$cachedNinoAndUsername")

  override def defaultSource: String = ???
  override def baseUri: String = ???
  override def domain: String = ???
  override def http: HttpGet with HttpPut with HttpDelete = ???

  private def loadObjectFromFile[T](filename: String)(implicit rds: Reads[T]): Option[T] = {
    val fileContents = Source.fromFile(filename).mkString
    Json.parse(fileContents).validate[T].fold(invalid => None, valid => Some(valid))
  }

  private def loadObjectBasedOnKey[T](key: String)(implicit rds: Reads[T]): Option[T] =
    key match {
      case _ => None
    }

  override def fetchAndGetEntry[T](key: String)(implicit hc: HeaderCarrier, rds: Reads[T],ec:ExecutionContext): Future[Option[T]] =
    Future.successful(hc.userId.filter(_ == cachedUserId).flatMap(p => loadObjectBasedOnKey(key)))

  override def cache[A](formId: String, body: A)(implicit wts: Writes[A], hc: HeaderCarrier,ec:ExecutionContext): Future[CacheMap] = Future.successful(CacheMap("", Map()))
}
