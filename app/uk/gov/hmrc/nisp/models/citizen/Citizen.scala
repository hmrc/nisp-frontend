/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.models.citizen

import uk.gov.hmrc.domain.Nino
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Citizen(nino:Nino, firstName: Option[String]=None, lastName: Option[String]=None, sex: Option[String]=None) {

  def getNameFormatted: Option[String] = {
    (firstName, lastName) match {
      case (Some(firstName), Some(lastName)) => Some("%s %s".format(firstName, lastName))
      case _ => None
    }
  }
}

object Citizen {
  implicit val formats: Format[Citizen] = (
    (__ \ "nino").format[Nino] and
    (__ \ "firstName").format[Option[String]] and
    (__ \ "lastName").format[Option[String]] and
    (__ \ "sex").format[Option[String]]
    )(Citizen.apply, unlift(Citizen.unapply))
}
