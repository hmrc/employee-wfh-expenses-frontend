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
import models.UserAnswers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import pages.NumberOfWeeksToClaimForPage
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository

import scala.concurrent.Future
import play.api.inject.bind
import navigation.{FakeNavigator, Navigator}
import play.api.mvc.Call
import services.SessionService

class ConfirmClaimInWeeksControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val confirmClaimInWeeksRoute = routes.ConfirmClaimInWeeksController.onPageLoad().url
  val numberOfWeeks = 2

  val form = new ConfirmClaimInWeeksFormProvider()(numberOfWeeks)

  "ConfirmClaimInWeeks Controller" must {
    "return OK and the correct view for a GET" in {
      val userAnswer = UserAnswers(userAnswersId, Json.obj(
        NumberOfWeeksToClaimForPage.toString -> numberOfWeeks
      ))
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

      val request = FakeRequest(GET, confirmClaimInWeeksRoute)

      val result = route(application, request).value

      status(result) mustEqual OK

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {

      val mockSessionService: SessionService = mock[SessionService]

      when(mockSessionService.set(any())(any())) thenReturn Future.successful(true)

      val userAnswer = UserAnswers(userAnswersId, Json.obj(
        NumberOfWeeksToClaimForPage.toString -> numberOfWeeks
      ))

      val application =
        applicationBuilder(userAnswers = Some(userAnswer))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionService].toInstance(mockSessionService)
          )
          .build()

      val request =
        FakeRequest(POST, confirmClaimInWeeksRoute)
          .withFormUrlEncodedBody(("value", true.toString))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "redirect to Session Expired for a GET if number of weeks is missing" in {

      val userAnswersMissingClaimWeeks = UserAnswers(userAnswersId, Json.obj(
        "some-key" -> "some-value"
      ))
      val application = applicationBuilder(userAnswers = Some(userAnswersMissingClaimWeeks)).build()

      val request = FakeRequest(GET, confirmClaimInWeeksRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad.url

      application.stop()
    }
  }
}
