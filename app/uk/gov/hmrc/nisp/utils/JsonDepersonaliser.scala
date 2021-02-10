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

package uk.gov.hmrc.nisp.utils

import play.api.libs.json._

import scala.util.Try

object JsonDepersonaliser {

  def depersonalise(json: JsValue): Try[String] = {
    Try(Json.prettyPrint(depersonaliseValue(json)))
  }

  def depersonaliseObject(obj: JsObject): JsObject = {

    val underlying: Map[String, JsValue] = (for {
      (key, value) <- obj.fields
    }
    yield {
      (key, depersonaliseValue(value))
    }).toMap

    JsObject(underlying)

  }

  def depersonaliseArray(array: JsArray): JsArray = {

    val value: Seq[JsValue] = for {
      value <- array.value
    }
    yield {
      depersonaliseValue(value)
    }

    JsArray(value)

  }

  def depersonaliseValue(value: JsValue): JsValue = {

    value match {
      case v: JsArray   => depersonaliseArray(v)
      case _: JsBoolean => JsBoolean(false)
      case v: JsNumber  => JsNumber(depersonaliseNumber(v.value))
      case v: JsObject  => depersonaliseObject(v)
      case v: JsString  => JsString(depersonaliseString(v.value))
      case JsNull       => JsNull
    }

  }

  def depersonaliseString(string: String): String = {
    string.replaceAll("[0-9]", "1").replaceAll("[a-zA-Z]", "a")
  }

  def depersonaliseNumber(number: BigDecimal): BigDecimal = {
    BigDecimal.apply(number.toString().replaceAll("[0-9]", "1"))
  }

}
