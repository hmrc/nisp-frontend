/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.views.html


import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, Entities}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.test.UnitSpec
import play.twirl.api.Html
import org.apache.commons.lang3.StringEscapeUtils
import org.jsoup.nodes
import org.jsoup.nodes.Document
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.safety.Whitelist
import uk.gov.hmrc.nisp.controllers.auth.NispUser
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, ConfidenceLevel, CredentialStrength, PayeAccount}

trait HtmlSpec extends UnitSpec {

  implicit val request = FakeRequest()

  implicit val authContext = {
    val user = LoggedInUser("", None, None, None, CredentialStrength.None, ConfidenceLevel.L500)
    val principal = Principal(None, accounts = Accounts(paye = Some(PayeAccount("", TestAccountBuilder.regularNino))))
    AuthContext(user, principal, None, None, None)
  }

  implicit val nispUser = NispUser(
    authContext = authContext,
    Some("First Last"),
    "",
    None,
    None,
    None
  )

  def asDocument(html: String): Document = Jsoup.parse(html)



  def assertEqualsMessage(doc: Document, cssSelector: String, expectedMessageKey: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
    assert(elements.first().html().replace("\n", "") == StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey)).toString())
  }

  def assertEqualsValue(doc: Document, cssSelector: String, expectedValue: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
    assert(elements.first().html().replace("\n", "") == expectedValue)
  }

  def assertMessageKeyHasValue(expectedMessageKey: String): Unit = {
    assert(expectedMessageKey != Html(Messages(expectedMessageKey)).toString(), s"$expectedMessageKey has no messages file value setup")
  }

  def assertContainsDynamicMessage(doc: Document, cssSelector: String, expectedMessageKey: String, messageArgs1: String ,messageArgs2: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey, messageArgs1, messageArgs2).toString())

    assert(StringEscapeUtils.unescapeHtml4((elements.first().html()).toString().replace("\n", "")) == expectedString)

  }

  def assertRenderedByCssSelector(doc: Document, cssSelector: String) = {
    assert(!doc.select(cssSelector).isEmpty, "Element " + cssSelector + " was not rendered on the page.")
  }

  def assertNotRenderedByCssSelector(doc: Document, cssSelector: String) = {
    assert(doc.select(cssSelector).isEmpty, "\n\nElement " + cssSelector + " was rendered on the page.\n")
  }


  def assertElementContainsText(doc: Document, cssSelector: String, text: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty)
      throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assert(elements.first().html().contains(text), s"\n\nText '$text' was not rendered inside '$cssSelector'.\n")
  }

  def assertContainsTextBetweenTags(doc: Document, cssSelector: String, expectedMessageKey: String , cssSelectorSecondElement :String) = {

    val elements = doc.select(cssSelector)
    val secondElement = doc.select(cssSelectorSecondElement);

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)

    val expectedString = StringEscapeUtils.unescapeHtml4(Messages(expectedMessageKey).toString())
    val elementText = elements.first().text().replace("\n", "");
    val secondElementText = secondElement.first().text().replace("\n", "");
    val mainElementText = elementText.replace(secondElementText, "");

    assert( StringEscapeUtils.unescapeHtml4(mainElementText.replace("\u00a0", "").toString()) == expectedString)

  }

  def assertLinkHasValue(doc: Document,cssSelector: String, linkValue: String) = {
    val elements = doc.select(cssSelector)
    assert(elements.attr("href") === linkValue)
  }


}
