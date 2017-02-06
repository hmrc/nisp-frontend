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
import org.jsoup.nodes.{Document, Element}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.test.UnitSpec
import play.twirl.api.Html
import org.apache.commons.lang3.StringEscapeUtils
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
    assert(elements.first().html().replace("\n", "") == StringEscapeUtils.escapeHtml4(Html(Messages(expectedMessageKey)).toString()))
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

  def assertContainsMessage(doc: Document, cssSelector: String, expectedMessageKey: String) = {
    assertContainsDynamicMessage(doc, cssSelector, expectedMessageKey)
  }

  def assertContainsDynamicMessage(doc: Document, cssSelector: String, expectedMessageKey: String, messageArgs: String*) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assertMessageKeyHasValue(expectedMessageKey)
    //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
    val expectedString = Html(Messages(expectedMessageKey, messageArgs: _*)).toString()

    assert(elements.toArray(new Array[Element](elements.size())).exists { element =>
      element.html().replace("\n", "").contains(StringEscapeUtils.escapeHtml4(expectedString))
    })
  }

  def assertLinkHasValue(doc: Document, id: String, linkValue: String) = {
    assert(doc.select(s"#$id").attr("href") === linkValue)
  }


  def assertElementContainsText(doc: Document, cssSelector: String, text: String) = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty)
      throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    assert(elements.first().html().contains(text), s"\n\nText '$text' was not rendered inside '$cssSelector'.\n")
  }


}
