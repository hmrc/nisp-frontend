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

package uk.gov.hmrc.nisp.controllers.auth

import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.{EmptyRetrieval, Retrieval}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder

import scala.concurrent.{ExecutionContext, Future}

trait MockAuthorisedFunctions extends AuthorisedFunctions {
  def authConnector: AuthConnector


  override def authorised(): AuthorisedFunction = new MockAuthorisedFunction(EmptyPredicate)

  override def authorised(predicate: Predicate): AuthorisedFunction = new MockAuthorisedFunction(predicate)


  class MockAuthorisedFunction(predicate: Predicate) extends AuthorisedFunction(predicate) {

    override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      authConnector.authorise(predicate, EmptyRetrieval).flatMap(_ => body)


    override def retrieve[A](retrieval: Retrieval[A]) = new MockAuthorisedFunctionWithResult(predicate, retrieval)

  }

  class MockAuthorisedFunctionWithResult[A](predicate: Predicate, retrieval: Retrieval[A]) extends AuthorisedFunctionWithResult(predicate, retrieval){

    val fr: Future[A] = Future.successful(new ~(Some(TestAccountBuilder.blankNino.nino), ConfidenceLevel.L100)).asInstanceOf[Future[A]]

    override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] =
      fr.flatMap(body)
  }
}
