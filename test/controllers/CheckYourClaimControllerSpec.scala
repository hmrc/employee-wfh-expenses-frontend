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
import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1, CurrentYearMinus2, CurrentYearMinus3, CurrentYearMinus4}
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SubmissionService

import scala.concurrent.Future

// scalastyle:off magic.number
class CheckYourClaimControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockSubmissionService: SubmissionService = mock[SubmissionService]

  override def beforeEach(): Unit = {
    reset(mockSubmissionService)
  }

  val fullUserAnswers: UserAnswers = UserAnswers(
    userAnswersId,
    Json.obj(
      SelectTaxYearsToClaimForPage.toString -> Json.arr(
        CurrentYear.toString,
        CurrentYearMinus1.toString,
        CurrentYearMinus2.toString,
        CurrentYearMinus3.toString,
        CurrentYearMinus4.toString
      ),
      NumberOfWeeksToClaimForPage.toString -> Json.arr(
        Json.arr(CurrentYear.toString, 1),
        Json.arr(CurrentYearMinus1.toString, 43),
      )
    )
  )
  val fullUserAnswersWithoutWeeks: UserAnswers = UserAnswers(
    userAnswersId,
    Json.obj(
      SelectTaxYearsToClaimForPage.toString -> Json.arr(
        CurrentYearMinus2.toString,
        CurrentYearMinus3.toString,
        CurrentYearMinus4.toString
      )
    )
  )
  val incompleteUserAnswers: UserAnswers = UserAnswers(
    userAnswersId,
    Json.obj(
      SelectTaxYearsToClaimForPage.toString -> Json.arr(
        CurrentYear.toString,
        CurrentYearMinus1.toString,
      ),
      NumberOfWeeksToClaimForPage.toString -> Json.arr(
        Json.arr(CurrentYear.toString, 1)
      )
    )
  )

  "CheckYourClaimController GET" must {
    "return OK with a view when everything is selected and non whole claim years have week data" in {
      val application = applicationBuilder(userAnswers = Some(fullUserAnswers)).build()
      val request = FakeRequest(GET, routes.CheckYourClaimController.onPageLoad().url)
      val result = route(application, request).value

      status(result) mustBe OK

      application.stop()
    }
    "return OK with a view when only whole claim years are selected" in {
      val application = applicationBuilder(userAnswers = Some(fullUserAnswersWithoutWeeks)).build()
      val request = FakeRequest(GET, routes.CheckYourClaimController.onPageLoad().url)
      val result = route(application, request).value

      status(result) mustBe OK

      application.stop()
    }
    "redirect to journey start if user does not have all expected week data" in {
      val application = applicationBuilder(userAnswers = Some(incompleteUserAnswers)).build()
      val request = FakeRequest(GET, routes.CheckYourClaimController.onPageLoad().url)
      val result = route(application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.IndexController.start.url)

      application.stop()
    }
  }

  "CheckYourClaimController POST" must {
    "redirect to confirmation page if submission is successful" in {
      when(mockSubmissionService.submitExpenses(any(), any())(any(), any(), any())) thenReturn Future.successful(Right(()))

      val application = applicationBuilder(userAnswers = Some(fullUserAnswers))
        .overrides(bind[SubmissionService].toInstance(mockSubmissionService))
        .build()
      val request = FakeRequest(POST, routes.CheckYourClaimController.onSubmit().url)
      val result = route(application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe routes.ConfirmationController.onPageLoad().url

      application.stop()
    }
    "redirect to confirmation page if submission with whole years only is successful" in {
      when(mockSubmissionService.submitExpenses(any(), any())(any(), any(), any())) thenReturn Future.successful(Right(()))

      val application = applicationBuilder(userAnswers = Some(fullUserAnswersWithoutWeeks))
        .overrides(bind[SubmissionService].toInstance(mockSubmissionService))
        .build()
      val request = FakeRequest(POST, routes.CheckYourClaimController.onSubmit().url)
      val result = route(application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe routes.ConfirmationController.onPageLoad().url

      application.stop()
    }
    "redirect to technical difficulties page if an error is thrown" in {
      when(mockSubmissionService.submitExpenses(any(), any())(any(), any(), any())) thenReturn Future.successful(Left(""))

      val application = applicationBuilder(userAnswers = Some(fullUserAnswers))
        .overrides(bind[SubmissionService].toInstance(mockSubmissionService))
        .build()
      val request = FakeRequest(POST, routes.CheckYourClaimController.onSubmit().url)
      val result = route(application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe routes.TechnicalDifficultiesController.onPageLoad.url

      application.stop()
    }
    "redirect to journey start if user does not have all expected week data" in {
      val application = applicationBuilder(userAnswers = Some(incompleteUserAnswers)).build()
      val request = FakeRequest(POST, routes.CheckYourClaimController.onPageLoad().url)
      val result = route(application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.IndexController.start.url)

      application.stop()
    }
  }
}