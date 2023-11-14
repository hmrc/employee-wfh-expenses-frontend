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

package repositories

import base.SpecBase
import config.FrontendAppConfig
import models.UserAnswers
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class SessionRepositorySpec extends SpecBase
  with FutureAwaits
  with DefaultAwaitTimeout
  with PlayMongoRepositorySupport[UserAnswers]
  with CleanMongoCollectionSupport
  with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val userId = "user-id"
  val anotherUserId = "another-user-id"

  val testData = Json.obj(
    "test" -> "data"
  )
  val testOtherData = Json.obj(
    "test" -> "otherData"
  )

  val testUserAnswers = UserAnswers(userId, testData)
  val testOtherUserAnswers = UserAnswers(anotherUserId, testOtherData)

  protected def checkTtlIndex: Boolean = true

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 30.seconds, interval = 100.millis)

  lazy val repository: SessionRepository = new SessionRepository(
    config = app.injector.instanceOf[FrontendAppConfig],
    mongoComponent = mongoComponent
  )



  ".get" must {
    "return None when the repository is empty" in {
      await(repository.get(anotherUserId)) mustBe None
    }
    "return the correct record from the repository" in {
      await(repository.set(testUserAnswers)) mustBe true
      await(repository.get(userId)).map(_.data) mustBe Some(testData)
      await(repository.get(userId)).map(_.lastUpdated) mustNot be (Some(testUserAnswers.lastUpdated))
    }
  }

  ".set" must {
    "populate the repository correctly" in {
      await(repository.set(testUserAnswers)) mustBe true
      await(repository.get(userId)).map(_.data) mustBe Some(testData)
      await(repository.get(userId)).map(_.lastUpdated) mustNot be (Some(testUserAnswers.lastUpdated))

      await(repository.get(anotherUserId)).map(_.data) mustBe None
    }
    "populate the repository correctly with multiple records" in {
      await(repository.set(testUserAnswers)) mustBe true
      await(repository.set(testOtherUserAnswers)) mustBe true

      await(repository.get(userId)).map(_.data) mustBe Some(testData)
      await(repository.get(userId)).map(_.lastUpdated) mustNot be (Some(testUserAnswers.lastUpdated))

      await(repository.get(anotherUserId)).map(_.data) mustBe Some(testOtherData)
      await(repository.get(anotherUserId)).map(_.lastUpdated) mustNot be (Some(testOtherUserAnswers.lastUpdated))

    }
  }
}