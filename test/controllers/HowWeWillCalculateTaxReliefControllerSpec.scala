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
import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1}
import models.UserAnswers
import pages.{ClaimedForTaxYear2020, SelectTaxYearsToClaimForPage}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class HowWeWillCalculateTaxReliefControllerSpec extends SpecBase {

  "HowWeWillCalculateTaxReliefPage Controller" must {

    "return the content view" when {
      val tests = Seq(
        (
          "not SA enrolled and has already claimed expenses for 2020", UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> true,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(CurrentYear.toString),
            )
          ),
          true,
          List(CurrentYear.toString)
        ) ,
        (
          "not SA enrolled and has already claimed expenses for multiple years", UserAnswers(
          userAnswersId,
          Json.obj(
            ClaimedForTaxYear2020.toString -> true,
            SelectTaxYearsToClaimForPage.toString -> Json.arr(CurrentYear.toString, CurrentYearMinus1.toString),
          )
        ),
          true,
          List(CurrentYear.toString, CurrentYearMinus1.toString)
        ) ,
        (
          "not SA enrolled and hasn't already claimed but have chosen only to claim for 2021", UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> false,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(CurrentYear.toString)
            )
          ),
          true,
          List(CurrentYear.toString)
        ),
        (
          "is SA enrolled and has already claimed expenses for 2020", UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> true,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(CurrentYear.toString),
            )
          ),
          true,
          List(CurrentYear.toString)),
        (
          "is SA enrolled and hasn't already claimed expenses for 2020", UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> false,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(CurrentYear.toString),
            )
          ),
          true,
          List(CurrentYear.toString)
        )
      )
      for((desc, userAnswer, _, _) <- tests) {
        s"$desc" in {
          val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

          val request = FakeRequest(GET, routes.HowWeWillCalculateTaxReliefController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK

          application.stop()
        }
      }
    }
  }
}
