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

package uk.gov.hmrc.nisp.utils

object Country {

  def isAbroad(countryName: String): Boolean =
    countryName match {
      case GREAT_BRITAIN    => false
      case ISLE_OF_MAN      => false
      case ENGLAND          => false
      case SCOTLAND         => false
      case WALES            => false
      case NORTHERN_IRELAND => false
      case NOT_SPECIFIED    => false
      case _                => true
    }

  private final val GREAT_BRITAIN    = "GREAT BRITAIN"
  private final val ISLE_OF_MAN      = "ISLE OF MAN"
  private final val ENGLAND          = "ENGLAND"
  private final val SCOTLAND         = "SCOTLAND"
  private final val WALES            = "WALES"
  private final val NORTHERN_IRELAND = "NORTHERN IRELAND"
  private final val NOT_SPECIFIED    = "NOT SPECIFIED OR NOT USED"

}
