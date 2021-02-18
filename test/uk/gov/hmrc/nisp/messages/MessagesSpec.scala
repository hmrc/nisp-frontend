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
import java.util.{Locale, Properties}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.Injecting
import resource._
import uk.gov.hmrc.play.test.UnitSpec
import scala.collection.JavaConversions._
import scala.io.{BufferedSource, Source}

class MessagesSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {

  val englishMessages: Set[String] = createMessageSet(new File(".", "conf/messages"))
  val welshMessages: Set[String] = createMessageSet(new File(".", "conf/messages.cy"))
  lazy val messagesApi: MessagesApi = inject[MessagesApi]
  lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), messagesApi)

  def createMessageSet(file: File): Set[String] = {
    if (file.exists()) {
      val properties = new Properties()
      val managedResource: ManagedResource[BufferedSource] = managed(Source.fromURI(file.toURI))
      managedResource.acquireAndGet(bufferedSource => properties.load(bufferedSource.bufferedReader()))
      properties.stringPropertyNames().toSet
    }
    else {
      throw new FileNotFoundException("Messages file cannot be loaded")
    }
  }

  def listMissingMessageKeys(header: String, missingKeys: Set[String]) =
    missingKeys.toList.sorted.mkString(s"$header\n", "\n", "\n"*2)

  "Application" should {
    "have the correct message configs" in {
      messagesApi.messages.size shouldBe 4
      messagesApi.messages.keys should contain theSameElementsAs Vector("en", "cy", "default", "default.play")
    }
  }

  "All message files" should {
    "have the same set of keys" in {
      englishMessages.size shouldBe welshMessages.size
    }
  }

  "English messages" should {
    "have the same keys as the welsh messages" in {
      withClue(listMissingMessageKeys("The following message keys are missing from English Set:", welshMessages.diff(englishMessages))) {
        assert(englishMessages equals welshMessages)
      }
    }
  }

  "Welsh message" should {
    "have the same keys as the english messages" in {
      withClue(listMissingMessageKeys("The following message keys are missing from Welsh Set:", englishMessages.diff(welshMessages))) {
        assert(welshMessages equals englishMessages)
      }
    }
  }
}