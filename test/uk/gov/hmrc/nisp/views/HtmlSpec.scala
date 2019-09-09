/*
 * Copyright 2019 HM Revenue & Customs
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

import org.apache.commons.lang3.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.nisp.controllers.auth.NispUser
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, ConfidenceLevel, CredentialStrength, PayeAccount}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}

trait HtmlSpec extends PlaySpec with OneAppPerSuite {

  implicit val request = FakeRequest()
  implicit val lang = LanguageToggle.getLanguageCode()
  implicit val lanCookie = LanguageToggle.getLanguageCookie()
  implicit val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit def messages = new Messages(lang, messagesApi)

  implicit val authContext = {
    val user = LoggedInUser("", None, None, None, CredentialStrength.None, ConfidenceLevel.L500, "test oid")
    val principal = Principal(None, accounts = Accounts(paye = Some(PayeAccount("", TestAccountBuilder.regularNino))))
    AuthContext(user, principal, None, None, None, None)
  }

  implicit val nispUser = NispUser(
    authContext = authContext,
    Some("First Last"),
    "",
    None,
    None
  )

  def asDocument(html: String): Document = Jsoup.parse(html)

  def assertEqualsMessage(doc: Document, cssSelector: String, expectedMessageKey: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
    assert(StringEscapeUtils.unescapeHtml4(elements.first().html().replace("\n", "")) == StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey)).toString())
  }

  def assertNotEqualsMessage(doc: Document, cssSelector: String, expectedMessageKey: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
    assert(StringEscapeUtils.unescapeHtml4(elements.first().html().replace("\n", "")) != StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey)).toString())
  }

  def assertEqualsValue(doc: Document, cssSelector: String, expectedValue: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
    assert(StringEscapeUtils.unescapeHtml4(elements.first().text().replace("\n", "")) == expectedValue)
  }

  def assertMessageKeyHasValue(expectedMessageKey: String): Unit = {
    assert(expectedMessageKey != Html(Messages(expectedMessageKey)).toString(), s"$expectedMessageKey has no messages file value setup")
  }

  def assertContainsDynamicMessage(doc: Document, cssSelector: String, expectedMessageKey: String, args: String*) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey, args: _*))
    assert(StringEscapeUtils.unescapeHtml4(elements.first().html().replace("\n", "")) == expectedString)
  }

  def assertDoesNotContainDynamicMessage(doc: Document, cssSelector: String, expectedMessageKey: String, args: String*) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey, args: _*))
    assert(StringEscapeUtils.unescapeHtml4(elements.first().html().replace("\n", "")) != expectedString)
  }

  def assertPageContainsDynamicMessage(doc: Document, expectedMessageKey: String, args: String*) = {
    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey, args: _*))
    assert(doc.text().contains(expectedString))
  }

  def assertPageDoesNotContainDynamicMessage(doc: Document, expectedMessageKey: String, args: String*) = {
    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey, args: _*))
    assert(!doc.text().contains(expectedString))
  }

  def assertPageContainsMessage(doc: Document, expectedMessage: String) = {
    assertMessageKeyHasValue(expectedMessage)
    assert(doc.text().contains(expectedMessage))
  }

  def assertPageDoesNotContainMessage(doc: Document, expectedMessage: String) = {
    assertMessageKeyHasValue(expectedMessage)
    assert(!doc.text().contains(expectedMessage))
  }

  def assertRenderedByCssSelector(doc: Document, cssSelector: String) = {
    assert(!doc.select(cssSelector).isEmpty, "Element " + cssSelector + " was not rendered on the page.")
  }

  def assertElementContainsText(doc: Document, cssSelector: String, text: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty)
      throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assert(elements.first().html().contains(text), s"\n\nText '$text' was not rendered inside '$cssSelector'.\n")
  }

  def assertContainsMessageBetweenTags(doc: Document, cssSelector: String, expectedMessageKey: String, cssSelectorSecondElement: String) = {

    val elements = doc.select(cssSelector)
    val secondElement = doc.select(cssSelectorSecondElement);

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey).toString())
    val elementText = elements.first().text().replace("\n", "");
    val secondElementText = secondElement.first().text().replace("\n", "");
    val mainElementText = elementText.replace(secondElementText, "");

    assert(StringEscapeUtils.unescapeHtml4(mainElementText.replace("\u00a0", "").toString()) == expectedString)
  }

  def assertContainsTextBetweenTags(doc: Document, cssSelector: String, expectedMessageValue: String, cssSelectorSecondElement: String) = {

    val elements = doc.select(cssSelector)
    val secondElement = doc.select(cssSelectorSecondElement);

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    val expectedString = expectedMessageValue
    val elementText = elements.first().text().replace("\n", "");
    val secondElementText = secondElement.first().text().replace("\n", "");
    val mainElementText = elementText.replace(secondElementText, "");

    assert(StringEscapeUtils.unescapeHtml4(mainElementText.replace("\u00a0", "").toString()) == expectedString)
  }

  def assertLinkHasValue(doc: Document, cssSelector: String, linkValue: String) = {
    val elements = doc.select(cssSelector)
    assert(elements.attr("href") === linkValue)
  }

  def assertContainsChildWithMessage(doc: Document, cssSelector: String, expectedMessageKey: String, messageArgs1: String, messageArgs2: String, messageArgs3: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)
    var sMessage = Messages(expectedMessageKey) + " " + Messages(messageArgs1) + " " + Messages(messageArgs2) + " " + Messages(messageArgs3);

    val expectedString = StringEscapeUtils.unescapeHtml4(sMessage.toString());
    assert(StringEscapeUtils.unescapeHtml4(elements.first().text().replace("\u00a0", "")) == expectedString.replace("\u00a0", ""))
  }

  def assertRenderedById(doc: Document, id: String) = {
    assert(doc.getElementById(id) != null, "\n\nElement " + id + " was not rendered on the page.\n")
  }

  def assertElementHasValue(doc: Document, id: String, value: String) = {
    assertRenderedById(doc, id)

    assert(doc.getElementById(id).attr("value") == value, s"\n\nElement $id has incorrect value. Expected '$value', found '${doc.getElementById(id).attr("value")}'.")
  }

  def assertElementNotContainsText(doc: Document, cssSelector: String, text: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty)
      throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assert(!elements.first().html().contains(text), s"\n\nText '$text' was rendered inside '$cssSelector'.\n")
  }

  def assertHasClass(doc: Document, cssSelector: String, className: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty)
      throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assert(elements.first().hasClass(className), s"\n\nElement '$cssSelector' doesn't have '$className' class.\n")
  }

  def assertDoesNotHaveClass(doc: Document, cssSelector: String, className: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty)
      throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assert(!elements.first().hasClass(className), s"\n\nElement '$cssSelector' has '$className' class.\n")
  }

  def assertElemetsOwnMessage(doc: Document, cssSelector: String, messageKey: String, stringValue: String = "") = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty)
      throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(messageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(messageKey).toString() + stringValue);
    assert(StringEscapeUtils.unescapeHtml4(elements.first().ownText().replace("\u00a0", "")) == expectedString.replace("\u00a0", ""))
  }

  def assertElemetsOwnText(doc: Document, cssSelector: String, expectedText: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty)
      throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    val expectedString = StringEscapeUtils.unescapeHtml4(expectedText);
    assert(StringEscapeUtils.unescapeHtml4(elements.first().ownText().replace("\u00a0", "")) == expectedString.replace("\u00a0", ""))
  }

  def assertContainsNextedValue(doc: Document, cssSelector: String, expectedMessageKey: String, secondMessageValue: String) = {

    val elements = doc.select(cssSelector)
    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey, secondMessageValue).toString())
    val elementText = elements.first().html().replace("\n", "");

    assert(StringEscapeUtils.unescapeHtml4(elementText.replace("\u00a0", "").toString()) == expectedString)
  }
}
