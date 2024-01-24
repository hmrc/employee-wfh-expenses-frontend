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
import forms.WhenDidYouFirstStartWorkingFromHomeFormProvider
import models.SelectTaxYearsToClaimFor.{Option1, Option2}
import models.UserAnswers
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{ClaimedForTaxYear2020, HasSelfAssessmentEnrolment, SelectTaxYearsToClaimForPage, WhenDidYouFirstStartWorkingFromHomePage}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SessionService

import java.time.LocalDate
import scala.concurrent.Future

class WhenDidYouFirstStartWorkingFromHomeControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new WhenDidYouFirstStartWorkingFromHomeFormProvider()

  private def form: Form[LocalDate] = formProvider()

  def onwardRoute: Call = Call("GET", "/foo")

  // scalastyle:off magic.number
  val validAnswer: LocalDate = LocalDate.of(2020, 1, 1)
  // scalastyle:on magic.number

  lazy val whenDidYouFirstStartWorkingFromHomeRoute: String = routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad().url

  override val emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  def getRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, whenDidYouFirstStartWorkingFromHomeRoute)

  def postRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, whenDidYouFirstStartWorkingFromHomeRoute)
      .withFormUrlEncodedBody(
        "value.day" -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year" -> validAnswer.getYear.toString
      )


  "WhenDidYouFirstStartWorkingFromHome Controller" when {
    "User has chosen previous tax years only" should {

      val answers: UserAnswers = UserAnswers(
        userAnswersId,
        Json.obj(
          ClaimedForTaxYear2020.toString -> false,
          HasSelfAssessmentEnrolment.toString -> false,
          SelectTaxYearsToClaimForPage.toString -> Json.arr(Option2.toString)
        )
      )

      "return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(answers)).build()
        val result = route(application, getRequest).value

        status(result) mustEqual OK

        application.stop()
      }

      "populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = answers.set(WhenDidYouFirstStartWorkingFromHomePage, validAnswer).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        val result = route(application, getRequest).value

        status(result) mustEqual OK

        application.stop()
      }

      "return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        val request =
          FakeRequest(POST, whenDidYouFirstStartWorkingFromHomeRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        application.stop()
      }
    }

    "User has chosen both previous and current tax years" should {

      val answers = UserAnswers(userAnswersId,
        Json.obj(
          ClaimedForTaxYear2020.toString -> false,
          HasSelfAssessmentEnrolment.toString -> false,
          SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString, Option2.toString)
        ))

      "return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(answers)).build()

        val result = route(application, getRequest).value

        status(result) mustEqual OK

        application.stop()
      }

      "populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = answers.set(WhenDidYouFirstStartWorkingFromHomePage, validAnswer).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
        val result = route(application, getRequest).value

        status(result) mustEqual OK

        application.stop()
      }

      "return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        val request =
          FakeRequest(POST, whenDidYouFirstStartWorkingFromHomeRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        application.stop()
      }
    }

    "redirect to the next page when valid data is submitted" in {

      val mockSessionService: SessionService = mock[SessionService]

      when(mockSessionService.set(any())(any())) thenReturn Future.successful(true)

      val userAnswers = UserAnswers(
        userAnswersId,
        Json.obj(
          ClaimedForTaxYear2020.toString -> false,
          HasSelfAssessmentEnrolment.toString -> false,
          SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString, Option2.toString)
        )
      ).set(WhenDidYouFirstStartWorkingFromHomePage, validAnswer).success.value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionService].toInstance(mockSessionService)
          )
          .build()

      val result = route(application, postRequest).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual onwardRoute.url

      application.stop()
    }

    "redirect to Session Expired for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val result = route(application, getRequest).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad.url

      application.stop()
    }

    "redirect to Session Expired for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      val result = route(application, postRequest).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad.url

      application.stop()
    }
  }

}
