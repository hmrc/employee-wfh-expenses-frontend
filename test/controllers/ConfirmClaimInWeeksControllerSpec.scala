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
import forms.ConfirmClaimInWeeksFormProvider
import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1}
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.NumberOfWeeksToClaimForPage
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SessionService

import scala.concurrent.Future

class ConfirmClaimInWeeksControllerSpec extends SpecBase with MockitoSugar {

  lazy val confirmClaimInWeeksRoute: String = routes.ConfirmClaimInWeeksController.onPageLoad().url
  val numberOfWeeks                         = 2

  val form = new ConfirmClaimInWeeksFormProvider()(numberOfWeeks)

  "ConfirmClaimInWeeksController GET" must {
    "return OK when user has multiple week claims" in {
      val userAnswers = UserAnswers(
        userAnswersId,
        Json.obj(
          NumberOfWeeksToClaimForPage.toString -> Json.arr(
            Json.arr(CurrentYear.toTaxYear.startYear, numberOfWeeks),
            Json.arr(CurrentYearMinus1.toTaxYear.startYear, numberOfWeeks)
          )
        )
      )
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      val request = FakeRequest(GET, confirmClaimInWeeksRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      application.stop()
    }

    "return OK when user has only one week claim" in {
      val userAnswers = UserAnswers(
        userAnswersId,
        Json.obj(
          NumberOfWeeksToClaimForPage.toString -> Json.arr(
            Json.arr(CurrentYearMinus1.toTaxYear.startYear, numberOfWeeks)
          )
        )
      )
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      val request = FakeRequest(GET, confirmClaimInWeeksRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      application.stop()
    }

    "redirect to Session Expired if number of weeks is missing" in {
      val userAnswersMissingClaimWeeks = UserAnswers(
        userAnswersId,
        Json.obj(
          "some-key" -> "some-value"
        )
      )
      val application = applicationBuilder(userAnswers = Some(userAnswersMissingClaimWeeks)).build()

      val request = FakeRequest(GET, confirmClaimInWeeksRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad.url

      application.stop()
    }
  }

  "ConfirmClaimInWeeksController POST" must {
    "redirect to the Check Your Claim page when valid data is submitted for multiple week claims" in {
      val mockSessionService: SessionService = mock[SessionService]
      when(mockSessionService.set(any())(any())).thenReturn(Future.successful(true))

      val userAnswers = UserAnswers(
        userAnswersId,
        Json.obj(
          NumberOfWeeksToClaimForPage.toString -> Json.arr(
            Json.arr(CurrentYear.toTaxYear.startYear, numberOfWeeks),
            Json.arr(CurrentYearMinus1.toTaxYear.startYear, numberOfWeeks)
          )
        )
      )

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionService].toInstance(mockSessionService))
        .build()
      val request = FakeRequest(POST, confirmClaimInWeeksRoute)
      val result  = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.CheckYourClaimController.onPageLoad().url

      application.stop()
    }
    "redirect to the Check Your Claim page when valid data is submitted for one week claim" in {
      val mockSessionService: SessionService = mock[SessionService]
      when(mockSessionService.set(any())(any())).thenReturn(Future.successful(true))

      val userAnswers = UserAnswers(
        userAnswersId,
        Json.obj(
          NumberOfWeeksToClaimForPage.toString -> Json.arr(
            Json.arr(CurrentYearMinus1.toTaxYear.startYear, numberOfWeeks)
          )
        )
      )

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionService].toInstance(mockSessionService))
        .build()
      val request = FakeRequest(POST, confirmClaimInWeeksRoute)
        .withFormUrlEncodedBody(("value", true.toString))
      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.CheckYourClaimController.onPageLoad().url

      application.stop()
    }
    "redirect to Session Expired page when the week count is missing" in {
      val mockSessionService: SessionService = mock[SessionService]

      val userAnswers = UserAnswers(userAnswersId, Json.obj())

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionService].toInstance(mockSessionService))
        .build()
      val request = FakeRequest(POST, confirmClaimInWeeksRoute)
      val result  = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad.url

      application.stop()
    }
  }

}
