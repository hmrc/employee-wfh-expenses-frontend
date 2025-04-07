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
import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1, CurrentYearMinus4}
import models.UserAnswers
import pages.{ClaimedForTaxYears, SelectTaxYearsToClaimForPage}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class HowWeWillCalculateTaxReliefControllerSpec extends SpecBase {

  "HowWeWillCalculateTaxReliefPage Controller" must {

    "return the content view" when {
      val tests = Seq(
        (
          "has already claimed expenses for CY-4",
          UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYears.toString           -> Json.arr(CurrentYearMinus4.toTaxYear.startYear),
              SelectTaxYearsToClaimForPage.toString -> Json.arr(CurrentYear.toTaxYear.startYear)
            )
          )
        ),
        (
          "has chosen to claim expenses for multiple years and has already claimed expenses for CY-4",
          UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYears.toString -> Json.arr(CurrentYearMinus4.toTaxYear.startYear),
              SelectTaxYearsToClaimForPage.toString -> Json
                .arr(CurrentYear.toTaxYear.startYear, CurrentYearMinus1.toTaxYear.startYear)
            )
          )
        ),
        (
          "hasn't already claimed but have chosen only to claim for CY",
          UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYears.toString           -> Json.arr(),
              SelectTaxYearsToClaimForPage.toString -> Json.arr(CurrentYear.toTaxYear.startYear)
            )
          )
        )
      )
      for ((desc, userAnswer) <- tests)
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
