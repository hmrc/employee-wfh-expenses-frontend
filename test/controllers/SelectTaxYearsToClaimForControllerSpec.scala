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
import forms.SelectTaxYearsToClaimForFormProvider
import models.{NormalMode, SelectTaxYearsToClaimFor, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{ClaimedForTaxYear2020, SelectTaxYearsToClaimForPage}
import play.api.inject.bind
import play.api.libs.json.{JsString, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.SelectTaxYearsToClaimForView

import scala.concurrent.Future

class SelectTaxYearsToClaimForControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val selectTaxYearsToClaimForRoute = routes.SelectTaxYearsToClaimForController.onPageLoad().url

  val formProvider = new SelectTaxYearsToClaimForFormProvider()
  val form = formProvider()

  "SelectTaxYearsToClaimFor Controller" must {

    "return OK and the correct view for a GET" in {
      val userAnswer = UserAnswers(userAnswersId, Json.obj(ClaimedForTaxYear2020.toString -> false))

      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

      val request = FakeRequest(GET, selectTaxYearsToClaimForRoute)

      val result = route(application, request).value

      val view = application.injector.instanceOf[SelectTaxYearsToClaimForView]

      status(result) mustEqual OK

      contentAsString(result) mustEqual
        view(form)(request, messages).toString

      application.stop()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswer = UserAnswers(
        userAnswersId,
        Json.obj(
          ClaimedForTaxYear2020.toString -> false,
          SelectTaxYearsToClaimForPage.toString -> SelectTaxYearsToClaimFor.values
        )
      )

      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

      val request = FakeRequest(GET, selectTaxYearsToClaimForRoute)

      val view = application.injector.instanceOf[SelectTaxYearsToClaimForView]

      val result = route(application, request).value

      status(result) mustEqual OK

      contentAsString(result) mustEqual
        view(form.fill(SelectTaxYearsToClaimFor.values.toSet))(request, messages).toString

      application.stop()
    }

    "redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      val request =
        FakeRequest(POST, selectTaxYearsToClaimForRoute)
          .withFormUrlEncodedBody(("value[0]", SelectTaxYearsToClaimFor.values.head.toString))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val request =
        FakeRequest(POST, selectTaxYearsToClaimForRoute)
          .withFormUrlEncodedBody(("value", "invalid value"))

      val boundForm = form.bind(Map("value" -> "invalid value"))

      val view = application.injector.instanceOf[SelectTaxYearsToClaimForView]

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) mustEqual
        view(boundForm)(request, messages).toString

      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request = FakeRequest(GET, selectTaxYearsToClaimForRoute)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad.url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val request =
        FakeRequest(POST, selectTaxYearsToClaimForRoute)
          .withFormUrlEncodedBody(("value[0]", SelectTaxYearsToClaimFor.values.head.toString))

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad.url

      application.stop()
    }
  }
}
