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
import models.UserAnswers
import pages.WhenDidYouFirstStartWorkingFromHomePage
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.TaxYearDates._
import views.html.{YourTaxRelief2019And2020View, YourTaxRelief2020OnlyView}

class YourTaxReliefControllerSpec extends SpecBase {

  "YourTaxReliefController" must {

    "for a GET" must {

      "redirect when no working from home date" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        val request = FakeRequest(GET, routes.YourTaxReliefController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DisclaimerController.onPageLoad().url

        application.stop()
      }

      "return OK and the correct view when a working from home start date is in the 2020-21 tax year" in {
        val workingFromHomeDate = TAX_YEAR_2020_START_DATE

        val userAnswers = UserAnswers(userAnswersId).set(WhenDidYouFirstStartWorkingFromHomePage, workingFromHomeDate).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        val request = FakeRequest(GET, routes.YourTaxReliefController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourTaxRelief2020OnlyView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(workingFromHomeDate)(request, messages).toString

        application.stop()
      }

      "return OK and the correct view when a working from home start date is in the 2019-20 tax year" in {
        val workingFromHomeDate = TAX_YEAR_2019_START_DATE

        val userAnswers = UserAnswers(userAnswersId).set(WhenDidYouFirstStartWorkingFromHomePage, workingFromHomeDate).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        val request = FakeRequest(GET, routes.YourTaxReliefController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourTaxRelief2019And2020View]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(workingFromHomeDate, 53)(request, messages).toString

        application.stop()
      }

    }

  }
}
