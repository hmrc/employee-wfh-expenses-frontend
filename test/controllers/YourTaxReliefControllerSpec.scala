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

package controllers

import base.SpecBase
import models.SelectTaxYearsToClaimFor.{Option1, Option2}
import models.UserAnswers
import pages.{ClaimedForTaxYear2020, SelectTaxYearsToClaimForPage, WhenDidYouFirstStartWorkingFromHomePage}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.{YourTaxRelief2019_2020View, YourTaxRelief2019_2020_2021View, YourTaxRelief2021OnlyView}

import java.time.LocalDate

class YourTaxReliefControllerSpec extends SpecBase {

  "YourTaxReliefController" must {

    "for a GET" must {

      "return the 2021 content view" when {
        val tests = Seq(
          (
            "already claimed expenses for 2020", UserAnswers(
              userAnswersId,
              Json.obj(ClaimedForTaxYear2020.toString -> true)
            )
          ),

          (
            "not already claimed but have chosen only to claim for 2021", UserAnswers(
              userAnswersId,
              Json.obj(
                ClaimedForTaxYear2020.toString -> false,
                SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString)
              )
            )
          )
        )
        for((desc, userAnswer) <- tests) {
          s"$desc" in {
            val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

            val view = application.injector.instanceOf[YourTaxRelief2021OnlyView]

            val request = FakeRequest(GET, routes.YourTaxReliefController.onPageLoad().url)

            val result = route(application, request).value

            status(result) mustEqual OK

            contentAsString(result) mustEqual
              view()(request, messages).toString

            application.stop()
          }
        }
      }

      "return the 2019 & 2020 content view" when {
        "not already claimed expenses for 2020 and tax years 2019 & 2020 have only been selected" in {
          val userAnswer = UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> false,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(Option2.toString),
              WhenDidYouFirstStartWorkingFromHomePage.toString -> earliestWorkingFromHomeDate
            )
          )

          val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

          val view = application.injector.instanceOf[YourTaxRelief2019_2020View]

          val request = FakeRequest(GET, routes.YourTaxReliefController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(earliestWorkingFromHomeDate, 14)(request, messages).toString

          application.stop()
        }
      }

      "return the 2019, 2020 & 2021 content view" when {
        "not already claimed expenses for 2020 and all tax years have been chosen" in {
          val userAnswer = UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> false,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString, Option2.toString),
              WhenDidYouFirstStartWorkingFromHomePage.toString -> earliestWorkingFromHomeDate
            )
          )

          val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

          val view = application.injector.instanceOf[YourTaxRelief2019_2020_2021View]

          val request = FakeRequest(GET, routes.YourTaxReliefController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(earliestWorkingFromHomeDate, 14)(request, messages).toString

          application.stop()
        }
      }

      "redirect to enter start date" when {
        "2019 and 2020 only but no start date is present" in {
          val userAnswer = UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> false,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(Option2.toString)
            )
          )

          val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

          val request = FakeRequest(GET, routes.YourTaxReliefController.onPageLoad().url)

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad().url

          application.stop()
        }

        "2019, 2020 and 2021 but no start date is present" in {
          val userAnswer = UserAnswers(
            userAnswersId,
            Json.obj(
              ClaimedForTaxYear2020.toString -> false,
              SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString, Option2.toString)
            )
          )

          val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

          val request = FakeRequest(GET, routes.YourTaxReliefController.onPageLoad().url)

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad().url

          application.stop()
        }
      }

    }

  }
}
