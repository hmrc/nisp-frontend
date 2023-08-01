/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.repositories.admin

import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.transaction.Transactions
import uk.gov.hmrc.nisp.models.admin.{FeatureFlag, FeatureFlagName}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureFlagRepository @Inject()(
  val mongoComponent: MongoComponent
)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[FeatureFlag](
      collectionName = "admin-feature-flags",
      mongoComponent = mongoComponent,
      domainFormat = FeatureFlag.format,
      indexes = Seq(
        IndexModel(
          keys = Indexes.ascending("name"),
          indexOptions = IndexOptions()
            .name("name")
            .unique(true)
        )
      ),
      extraCodecs = Codecs.playFormatSumCodecs(FeatureFlagName.formats)
    )
    with Transactions {

  def deleteFeatureFlag(name: FeatureFlagName): Future[Boolean] =
    collection
      .deleteOne(Filters.equal("name", name.toString))
      .map(_.wasAcknowledged())
      .toSingle()
      .toFuture()

  def getFeatureFlag(name: FeatureFlagName): Future[Option[FeatureFlag]] =
    collection
      .find(Filters.equal("name", name.toString))
      .headOption()

  def getAllFeatureFlags: Future[List[FeatureFlag]] =
    collection
      .find()
      .toFuture()
      .map(_.toList)

  def setFeatureFlag(name: FeatureFlagName, enabled: Boolean): Future[Boolean] =
    collection
      .replaceOne(
        filter = equal("name", name),
        replacement = FeatureFlag(name, enabled),
        options = ReplaceOptions().upsert(true)
      )
      .map(_.wasAcknowledged())
      .toSingle()
      .toFuture()

  def setFeatureFlags(flags: Map[FeatureFlagName, Boolean]): Future[Unit] = {
    val featureFlags: Seq[FeatureFlag] = flags.map { case (flag, status) =>
      FeatureFlag(flag, status, flag.description)
    }.toList

    val bulkWrites: Seq[ReplaceOneModel[FeatureFlag]] = featureFlags.map { flag =>
      ReplaceOneModel[FeatureFlag](equal("name", flag.name), flag, ReplaceOptions().upsert(true))
    }

    collection.bulkWrite(bulkWrites).toFuture().map(_ => ())
  }
}