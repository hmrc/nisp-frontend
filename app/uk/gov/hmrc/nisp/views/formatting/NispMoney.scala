/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.views.formatting

import play.twirl.api.Html

import scala.math.BigDecimal.RoundingMode._

object NispMoney {
  def pounds(
    value: BigDecimal,
    roundUp: Boolean = false
  ): Html = {
    val prefix: String =
      if (value < 0) "&minus;" else ""

    val decimalPlaces: Int =
      if (value.isValidInt) 0 else 2

    val rounding: Value =
      if (roundUp) CEILING else FLOOR

    def quantity: String =
      s"%,.${decimalPlaces}f"
        .format(
          value
            .setScale(decimalPlaces, rounding)
            .abs
        )

    Html(s"$prefix&pound;$quantity")
  }
}
