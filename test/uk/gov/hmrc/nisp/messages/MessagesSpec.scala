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

package uk.gov.hmrc.nisp.messages

import java.io.{File, FileNotFoundException}
import java.util.Properties

import resource._
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.JavaConversions._
import scala.io.{BufferedSource, Source}

class MessagesSpec extends UnitSpec {

  val propertiesEnglish: Properties = new Properties()
  val propertiesWelsh: Properties = new Properties()

  def loadProperties(file: File, properties: Properties) = {
    if (file.exists()) {
      val managedResource: ManagedResource[BufferedSource] = managed(Source.fromURI(file.toURI))
      managedResource.acquireAndGet(bufferedSource => properties.load(bufferedSource.bufferedReader()))
    }
    else {
      throw new FileNotFoundException("Messages file cannot be loaded")
    }
  }

  //TODO fix message files and output key differences
  "Messages" should {

    loadProperties(new File(".", "conf/messages"), propertiesEnglish)
    loadProperties(new File(".", "conf/messages.cy"), propertiesWelsh)

    "assert equal messages key for English" ignore {
      propertiesEnglish.stringPropertyNames().size() should be(propertiesWelsh.stringPropertyNames().size())
      propertiesEnglish.stringPropertyNames().foreach(key => {
        val englishContent = propertiesEnglish.getProperty(key, key)
        val welshContent = propertiesWelsh.getProperty(key, key)
        englishContent should not be key
        welshContent should not be key
      })
    }

    "assert equal messages key for Welsh" ignore {
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