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

package uk.gov.hmrc.nisp.views

import org.apache.commons.text.StringEscapeUtils
import org.apache.pekko.util.Timeout
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.nisp.utils.UnitSpec

import java.util.Locale
import scala.concurrent.duration._

trait HtmlSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterEach {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val defaultAwaitTimeout: Timeout = 5.seconds
  implicit lazy val messages: MessagesImpl  = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  val forceScaMock: Boolean = true

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset()
  }

  def asDocument(html: String): Document = Jsoup.parse(html)

  def assertEqualsMessage(doc: Document, cssSelector: String, expectedMessageKey: String, messageValue: Option[Int] = None): Assertion = {
    val elements = doc.select(cssSelector)
    val messageText = messageValue.fold(
      Messages(expectedMessageKey)
    )(
      value => Messages(expectedMessageKey, value)
    )

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    assert(
      StringEscapeUtils.unescapeHtml4(elements.first().html().replace("\n", "")) == StringEscapeUtils
        .unescapeHtml4(messageText)
    )
  }

  def assertEqualsText(doc: Document, cssSelector: String, expectedText: String): Assertion = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assert(
      StringEscapeUtils.unescapeHtml4(elements.first().text().replace("\n", "")) ==
        StringEscapeUtils.unescapeHtml4(expectedText)
    )
  }

  def assertEqualsValue(doc: Document, cssSelector: String, expectedValue: String): Assertion = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assert(StringEscapeUtils.unescapeHtml4(elements.first().text().replace("\n", "")) == expectedValue)
  }

  def assertMessageKeyHasValue(expectedMessageKey: String): Unit =
    assert(
      expectedMessageKey != Html(Messages(expectedMessageKey)).toString(),
      s"$expectedMessageKey has no messages file value setup"
    )

  def assertContainsDynamicMessage(doc: Document, cssSelector: String, expectedMessageKey: String, args: String*): Assertion = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey, args: _*))
    assert(StringEscapeUtils.unescapeHtml4(elements.first().html().replace("\n", "")) == expectedString)
  }

  def assertContainsDynamicMessageUsingClass(
    doc: Document,
    className: String,
    expectedMessageKey: String,
    args: String*
  ): Assertion = {
    val elements = doc.getElementsByClass(className)

    if (elements === null) throw new IllegalArgumentException(s"Class Selector $className wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey, args: _*))
    assert(StringEscapeUtils.unescapeHtml4(elements.first().html().replace("\n", "")) == expectedString)
  }

  def assertPageDoesNotContainDynamicMessage(doc: Document, expectedMessageKey: String, args: String*): Assertion = {
    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey, args: _*))
    assert(!doc.text().contains(expectedString))
  }

  def assertPageDoesNotContainMessage(doc: Document, expectedMessage: String): Assertion = {
    assertMessageKeyHasValue(expectedMessage)
    assert(!doc.text().contains(expectedMessage))
  }

  def assertElementContainsText(doc: Document, cssSelector: String, text: String): Assertion = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty)
      throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assert(elements.first().html().contains(text), s"\n\nText '$text' was not rendered inside '$cssSelector'.\n")
  }

  def assertContainsMessageBetweenTags(
    doc: Document,
    cssSelector: String,
    expectedMessageKey: String,
    cssSelectorSecondElement: String
  ): Assertion = {

    val elements      = doc.select(cssSelector)
    val secondElement = doc.select(cssSelectorSecondElement)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString    = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey))
    val elementText       = elements.first().text().replace("\n", "")
    val secondElementText = secondElement.first().text().replace("\n", "")
    val mainElementText   = elementText.replace(secondElementText, "")

    assert(StringEscapeUtils.unescapeHtml4(mainElementText.replace("\u00a0", "")) == expectedString)
  }

  def assertLinkHasValue(doc: Document, cssSelector: String, linkValue: String): Assertion = {
    val elements = doc.select(cssSelector)
    assert(elements.attr("href") === linkValue)
  }

  def assertContainsChildWithMessage(
    doc: Document,
    cssSelector: String,
    expectedMessageKey: String,
    messageArgs1: String,
    messageArgs2: String,
    messageArgs3: String
  ): Assertion = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)
    val sMessage =
      Messages(expectedMessageKey) + " " + Messages(messageArgs1) + " " + Messages(messageArgs2) + " " + Messages(
        messageArgs3
      )

    val expectedString = StringEscapeUtils.unescapeHtml4(sMessage)
    assert(
      StringEscapeUtils.unescapeHtml4(elements.first().text().replace("\u00a0", "")) == expectedString
        .replace("\u00a0", "")
    )
  }

  def assertElementsOwnMessage(doc: Document, cssSelector: String, messageKey: String, stringValue: String = ""): Assertion = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty)
      throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(messageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(messageKey) + stringValue)
    assert(
      StringEscapeUtils.unescapeHtml4(elements.first().ownText().replace("\u00a0", "")) == expectedString
        .replace("\u00a0", "")
    )
  }

  def assertElementsOwnText(doc: Document, cssSelector: String, expectedText: String): Assertion = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty)
      throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    val expectedString = StringEscapeUtils.unescapeHtml4(expectedText)
    assert(
      StringEscapeUtils.unescapeHtml4(elements.first().ownText().replace("\u00a0", "")) == expectedString
        .replace("\u00a0", "")
    )
  }

  def assertContainsExpectedValue(
    doc: Document,
    cssSelector: String,
    expectedMessageKey: String,
    secondMessageValue: String
  ): Assertion = {

    val elements = doc.select(cssSelector)
    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey, secondMessageValue))
    val elementText    = elements.first().html().replace("\n", "")

    assert(StringEscapeUtils.unescapeHtml4(elementText.replace("\u00a0", "")) == expectedString)
  }
}
