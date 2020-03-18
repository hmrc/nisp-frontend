/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.messages

import java.io.{File, FileNotFoundException}
import java.util.Properties
import scala.collection.JavaConversions._

import uk.gov.hmrc.play.test.UnitSpec

import scala.io.Source

class MessagesSpec extends UnitSpec {

  val englishFile = new File(".", "conf/messages")
  val welshFile = new File(".", "conf/messages.cy")
  val propertiesEnglish: Properties = new Properties()
  val propertiesWelsh: Properties = new Properties()

  "Messages" should {
    if (englishFile.exists()) {
      val sourceEnglish = Source.fromURI(englishFile.toURI)
      propertiesEnglish.load(sourceEnglish.bufferedReader())
    }
    else {
      throw new FileNotFoundException("Messages file cannot be loaded")
    }
    if (welshFile.exists()) {
      val sourceWelsh = Source.fromURI(welshFile.toURI)
      propertiesWelsh.load(sourceWelsh.bufferedReader())
    }
    else {
      throw new FileNotFoundException("Messages.cy file can not be loaded")
    }

    "assert equal messages key for English" in {
      propertiesEnglish.stringPropertyNames().size() should be(propertiesWelsh.stringPropertyNames().size())
      propertiesEnglish.stringPropertyNames().foreach(key => {
        val englishContent = propertiesEnglish.getProperty(key, key)
        val welshContent = propertiesWelsh.getProperty(key, key)
        englishContent should not be key
        welshContent should not be key
      })
    }

    "assert equal messages key for Welsh" in {
      propertiesEnglish.stringPropertyNames().size() should be(propertiesWelsh.stringPropertyNames().size())
      propertiesWelsh.stringPropertyNames().foreach(key => {
        val englishContent = propertiesEnglish.getProperty(key, key)
        val welshContent = propertiesWelsh.getProperty(key, key)
        englishContent should not be key
        welshContent should not be key
      })
    }
  }
}


