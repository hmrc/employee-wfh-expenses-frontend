/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.{LocalDate, ZoneOffset}

import base.SpecBase
import models.UserAnswers
import pages.WhenDidYouFirstStartWorkingFromHomePage
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.YourTaxReliefView

class YourTaxReliefControllerSpec extends SpecBase {

  val workingFromHomeDate = LocalDate.of(2019,4,6)

  "YourTaxRelief Controller" must {

    "for a GET" must {

      "redirect when no working from home date" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        val request = FakeRequest(GET, routes.YourTaxReliefController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourTaxReliefView]

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad().url

        application.stop()
      }

      "return OK and the correct view when a working from home date is present" in {
        val workingFromHomeDate = LocalDate.now(ZoneOffset.UTC)

        val userAnswers = UserAnswers(userAnswersId).set(WhenDidYouFirstStartWorkingFromHomePage, workingFromHomeDate).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        val request = FakeRequest(GET, routes.YourTaxReliefController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourTaxReliefView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(workingFromHomeDate)(fakeRequest, messages).toString

        application.stop()
      }

    }

  }
}
