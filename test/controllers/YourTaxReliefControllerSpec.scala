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

import models.UserAnswers
import pages.WhenDidYouFirstStartWorkingFromHomePage
import base.SpecBase
import connectors.PaperlessPreferenceConnector
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import views.html.{ConfirmationView, TechnicalErrorView, YourTaxReliefView}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SubmissionService

import scala.concurrent.Future

class YourTaxReliefControllerSpec extends SpecBase with MockitoSugar{

  val workingFromHomeDate = LocalDate.of(2019, 4, 6)
  private val paperlessPreferenceConnector = mock[PaperlessPreferenceConnector]
  private val submissionService = mock[SubmissionService]

  "YourTaxRelief Controller" must {

    "for a GET" must {

      "redirect when no working from home date" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        val request = FakeRequest(GET, routes.YourTaxReliefController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourTaxReliefView]

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DisclaimerController.onPageLoad().url

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

    "for a POST" must {
      "return OK and the correct view with submission and paper preferences available" in {
        val workingFromHomeDate = LocalDate.now(ZoneOffset.UTC)

        val userAnswers = UserAnswers(userAnswersId).set(WhenDidYouFirstStartWorkingFromHomePage, workingFromHomeDate).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[PaperlessPreferenceConnector].toInstance(paperlessPreferenceConnector))
          .overrides(bind[SubmissionService].toInstance(submissionService))
          .build()

        when(paperlessPreferenceConnector.getPaperlessPreference()(any(), any())) thenReturn
          Future.successful(Some(true))
        when(submissionService.submitExpenses(any())(any(),any(), any())) thenReturn
          Future.successful(Right())

        val request = FakeRequest(POST, routes.YourTaxReliefController.onSubmit().url)

        val result = route(application, request).value

        redirectLocation(result).value mustEqual routes.ConfirmationPaperlessController.onPageLoad().url

        application.stop()
      }

      "return failure page when submission failed but with preferences available" in {
        val userAnswers = UserAnswers(userAnswersId).set(WhenDidYouFirstStartWorkingFromHomePage, workingFromHomeDate).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[PaperlessPreferenceConnector].toInstance(paperlessPreferenceConnector))
          .build()

        when(paperlessPreferenceConnector.getPaperlessPreference()(any(), any())) thenReturn
          Future.successful(Some(true))

        val request = FakeRequest(POST, routes.YourTaxReliefController.onSubmit().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TechnicalErrorView]

        status(result) mustEqual SEE_OTHER

        contentAsString(result) mustEqual
          view()(fakeRequest, messages).toString

        application.stop()
      }

      "return OK and the correct view with paper preferences unavailable" in {
        paperlessControllerTest(false)
      }
    }}

    def paperlessControllerTest(paperlessAvailable: Boolean): Future[_] = {

      val userAnswers = UserAnswers(userAnswersId).set(WhenDidYouFirstStartWorkingFromHomePage, workingFromHomeDate).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[PaperlessPreferenceConnector].toInstance(paperlessPreferenceConnector))
        .build()

      when(paperlessPreferenceConnector.getPaperlessPreference()(any(), any())) thenReturn
        Future.successful(Some(paperlessAvailable))

      val request = FakeRequest(POST, routes.YourTaxReliefController.onSubmit().url)

      val result = route(application, request).value

      val view = application.injector.instanceOf[ConfirmationView]

      status(result) mustEqual OK

      contentAsString(result) mustEqual
        view(paperlessAvailable, None)(fakeRequest, messages).toString

      application.stop()
    }
  }
