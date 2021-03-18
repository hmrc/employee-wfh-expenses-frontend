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

package controllers.actions

import base.SpecBase
import controllers.Assets.SEE_OTHER
import controllers.routes
import models.UserAnswers
import models.requests.{DataRequest, OptionalDataRequest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import pages.SubmittedClaim
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRequiredActionSpec extends SpecBase with MockitoSugar with ScalaFutures {

  class FilterUnderTest extends DataRequiredActionImpl {
    def callRefine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]]= refine(request)
  }

  "DataRequiredAction" should {
    "redirect to the session expired page" when {
      "there are no userAnswers in the session" in {
        val optionalDataRequest = OptionalDataRequest(FakeRequest("GET", "/"), "internalId", None, "NINO")
        val futureResult = new FilterUnderTest().callRefine(optionalDataRequest)

        whenReady(futureResult) { result =>
          result.isLeft mustBe true
          result.left.get.header.status mustBe SEE_OTHER
          result.left.get.header.headers.get(LOCATION).contains(routes.SessionExpiredController.onPageLoad().url) mustBe true
        }
      }
    }

    "redirect to the confirmation page" when {
      "the SUBMITTEDCLAIM session member is present" in {
        val fakeRequest = FakeRequest("GET", routes.DisclaimerController.onPageLoad().url)

        val userAnswers = UserAnswers(
          userAnswersId,
          Json.obj(SubmittedClaim.toString -> true)
        )

        val optionalDataRequest = OptionalDataRequest(fakeRequest, "internalId", Some(userAnswers), "NINO")
        val futureResult = new FilterUnderTest().callRefine(optionalDataRequest)

        whenReady(futureResult) { result =>
          result.isLeft mustBe true
          result.left.get.header.status mustBe SEE_OTHER
          result.left.get.header.headers.get(LOCATION).contains(routes.ConfirmationController.onPageLoad().url) mustBe true
        }
      }
    }

    "not redirect to the confirmation page" when {
      "the SUBMITTEDCLAIM session member is present and the current URL is the confirmation page" in {
        val fakeRequest = FakeRequest("GET", routes.ConfirmationController.onPageLoad().url)

        val userAnswers = UserAnswers(
          userAnswersId,
          Json.obj(SubmittedClaim.toString -> true)
        )

        val optionalDataRequest = OptionalDataRequest(fakeRequest, "internalId", Some(userAnswers), "NINO")
        val futureResult = new FilterUnderTest().callRefine(optionalDataRequest)

        whenReady(futureResult) { result =>
          result.isRight mustBe true
        }
      }
    }
  }

}
