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

package controllers

import base.SpecBase
import models.SelectTaxYearsToClaimFor.{Option1, Option2}
import models.UserAnswers
import pages.{ClaimedForTaxYear2020, SelectTaxYearsToClaimForPage}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class DisclaimerControllerSpec extends SpecBase {

  "DisclaimerController" must {

    "return content view for both message sections" in {
      val userAnswers = UserAnswers(
        userAnswersId,
        Json.obj(
          ClaimedForTaxYear2020.toString -> true,
          SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString, Option2.toString),
        )
      )

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      val request = FakeRequest(GET, routes.DisclaimerController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual OK

      application.stop()

    }

    "return content view for both first sections only" in {
      val userAnswers = UserAnswers(
        userAnswersId,
        Json.obj(
          ClaimedForTaxYear2020.toString -> true,
          SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString),
        )
      )

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      val request = FakeRequest(GET, routes.DisclaimerController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual OK

      application.stop()

    }
  }
}
