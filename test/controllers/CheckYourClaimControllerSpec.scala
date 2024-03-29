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

import java.time.LocalDate

import base.SpecBase
import models.SelectTaxYearsToClaimFor.{Option1, Option2, Option3, Option4}
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SubmissionService

import scala.concurrent.Future

// scalastyle:off magic.number
class CheckYourClaimControllerSpec extends SpecBase with MockitoSugar {

  "Check your claim controller" must {
    "display when all available options are selected" in {

      val userAnswer = UserAnswers(
        userAnswersId,
        Json.obj(
          ClaimedForTaxYear2020.toString -> false,
          SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString, Option2.toString, Option3.toString, Option4.toString),
          WhenDidYouFirstStartWorkingFromHomePage.toString -> LocalDate.of(2020, 4, 1),
          NumberOfWeeksToClaimForPage.toString -> 3
        )
      )

      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

      val request = FakeRequest(GET, routes.CheckYourClaimController.onPageLoad().url)

      val result = route(application, request).value

      status(result) mustEqual OK

      application.stop()
    }

    "submit claim should redirect to technical difficulties page if results into error" in {

      val mockSubmissionService = mock[SubmissionService]

      when(mockSubmissionService.submitExpenses(any(), any(), any())(any(), any(), any())) thenReturn Future.successful(Left("dd"))

      val userAnswer = UserAnswers(
        userAnswersId,
        Json.obj(
          ClaimedForTaxYear2020.toString -> false,
          SelectTaxYearsToClaimForPage.toString -> Json.arr(Option2.toString)
        )
      )

      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

      val request = FakeRequest(POST, routes.CheckYourClaimController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.TechnicalDifficultiesController.onPageLoad.url

      application.stop()
    }
  }
}