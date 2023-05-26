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
import models.{ClaimViewSettings, DisclaimerViewSettings, TaxYearFromUIAssembler, UserAnswers}
import pages.{ClaimedForTaxYear2020, HasSelfAssessmentEnrolment, SelectTaxYearsToClaimForPage}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.DateLanguageTokenizer
import views.html.HowWeWillCalculateTaxReliefView

import java.time.LocalDate

class HowWeWillCalculateTaxReliefControllerSpec extends SpecBase {

  "HowWeWillCalculateTaxReliefPage Controller" must {

    "return the content view" when {
      val tests = Seq(
        (
          "not SA enrolled and has already claimed expenses for 2020", UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> true,
              HasSelfAssessmentEnrolment.toString -> false,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString),
            )
          ),
          true,
          List(Option1.toString)
        ) ,
        (
          "not SA enrolled and has already claimed expenses for multiple years", UserAnswers(
          userAnswersId,
          Json.obj(
            ClaimedForTaxYear2020.toString -> true,
            HasSelfAssessmentEnrolment.toString -> false,
            SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString, Option2.toString),
          )
        ),
          true,
          List(Option1.toString, Option2.toString)
        ) ,
        (
          "not SA enrolled and hasn't already claimed but have chosen only to claim for 2021", UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> false,
              HasSelfAssessmentEnrolment.toString -> false,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString)
            )
          ),
          true,
          List(Option1.toString)
        ),
        (
          "is SA enrolled and has already claimed expenses for 2020", UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> true,
              HasSelfAssessmentEnrolment.toString -> true,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString),
            )
          ),
          true,
          List(Option1.toString)),
        (
          "is SA enrolled and hasn't already claimed expenses for 2020", UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> false,
              HasSelfAssessmentEnrolment.toString -> true,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString),
            )
          ),
          true,
          List(Option1.toString)
        )
      )
      for((desc, userAnswer, backLinkEnabled, selectedDatesAsOptions: List[String]) <- tests) {
        s"$desc" in {
          val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

          val request = FakeRequest(GET, routes.HowWeWillCalculateTaxReliefController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK

          val assembler = TaxYearFromUIAssembler(selectedDatesAsOptions)

          val disclaimerViewSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble), None)))

          val date = LocalDate.of(2021, 4, 1)

          application.stop()
        }
      }
    }
  }
}
