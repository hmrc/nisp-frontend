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

package uk.gov.hmrc.nisp.events

import uk.gov.hmrc.play.audit.model.DataEvent

import scala.util.Try
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.ForwardedFor

abstract class NispBusinessEvent(auditType: String, detail: Map[String, String])(implicit hc: HeaderCarrier)
  extends DataEvent(auditSource = "nisp-frontend", auditType = auditType, detail = detail, tags =
    Map(
      "clientIP" -> (hc.forwarded match {
        case Some(ForwardedFor(someVal)) => Try(someVal.split(',').head).getOrElse("")
        case None => ""
      }),
      "X-Request-ID" -> hc.requestId.map(_.value).getOrElse(""),
      "X-Session-ID" -> hc.sessionId.map(_.value).getOrElse("")
    ))
