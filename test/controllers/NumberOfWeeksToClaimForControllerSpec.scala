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
import forms.SelectTaxYearsToClaimForFormProvider
import models.TaxYearSelection.{CurrentYearMinus4, _}
import models.{TaxYearSelection, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SessionService

import scala.collection.immutable.ListMap
import scala.concurrent.Future

class NumberOfWeeksToClaimForControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  lazy val testRoute: String = routes.NumberOfWeeksToClaimForController.onPageLoad().url
  val mockSessionService: SessionService = mock[SessionService]

  val formProvider = new SelectTaxYearsToClaimForFormProvider()

  override def beforeEach(): Unit = {
    reset(mockSessionService)
  }

  s"NumberOfWeeksToClaimForController GET" when {
    "user has not selected any years with week claims" must {
      "redirect to journey start as the user is page hopping" in {
        val userAnswer = UserAnswers(userAnswersId)
          .set(SelectTaxYearsToClaimForPage, Seq(CurrentYearMinus4)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
        val request = FakeRequest(GET, testRoute)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IndexController.start.url)

        application.stop()
      }
    }
    "user has selected current year" must {
      "return an OK with the view" in {
        val userAnswer = UserAnswers(userAnswersId)
          .set(SelectTaxYearsToClaimForPage, Seq(CurrentYear)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
        val request = FakeRequest(GET, testRoute)
        val result = route(application, request).value

        status(result) mustBe OK
        prepopulatedValue(result, CurrentYear.toTaxYear.startYear.toString) mustBe ""

        application.stop()
      }
      "return an OK with the view with existing answer" in {
        val prepopAnswer: ListMap[TaxYearSelection, Int] = ListMap(CurrentYear -> 30)
        val userAnswer = UserAnswers(userAnswersId)
          .set(NumberOfWeeksToClaimForPage, prepopAnswer)(NumberOfWeeksToClaimForPage.format).success.value
          .set(SelectTaxYearsToClaimForPage, Seq(CurrentYear)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
        val request = FakeRequest(GET, testRoute)
        val result = route(application, request).value

        status(result) mustBe OK
        prepopulatedValue(result, CurrentYear.toTaxYear.startYear.toString) mustBe "30"

        application.stop()
      }
    }
    "user has selected previous year" must {
      "return an OK with the view" in {
        val userAnswer = UserAnswers(userAnswersId)
          .set(SelectTaxYearsToClaimForPage, Seq(CurrentYearMinus1)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
        val request = FakeRequest(GET, testRoute)
        val result = route(application, request).value

        status(result) mustBe OK
        prepopulatedValue(result, CurrentYearMinus1.toTaxYear.startYear.toString) mustBe ""

        application.stop()
      }
      "return an OK with the view with existing answer" in {
        val prepopAnswer: ListMap[TaxYearSelection, Int] = ListMap(CurrentYearMinus1 -> 50)
        val userAnswer = UserAnswers(userAnswersId)
          .set(NumberOfWeeksToClaimForPage, prepopAnswer)(NumberOfWeeksToClaimForPage.format).success.value
          .set(SelectTaxYearsToClaimForPage, Seq(CurrentYearMinus1)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
        val request = FakeRequest(GET, testRoute)
        val result = route(application, request).value

        status(result) mustBe OK
        prepopulatedValue(result, CurrentYearMinus1.toTaxYear.startYear.toString) mustBe "50"

        application.stop()
      }
    }
    "user has selected multiple years with week based claims" must {
      "return an OK with the view" in {
        val userAnswer = UserAnswers(userAnswersId)
          .set(SelectTaxYearsToClaimForPage, Seq(CurrentYear, CurrentYearMinus1)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
        val request = FakeRequest(GET, testRoute)
        val result = route(application, request).value

        status(result) mustBe OK
        prepopulatedValue(result, CurrentYear.toTaxYear.startYear.toString) mustBe ""
        prepopulatedValue(result, CurrentYearMinus1.toTaxYear.startYear.toString) mustBe ""

        application.stop()
      }
      "return an OK with the view with existing answer" in {
        val prepopAnswer: ListMap[TaxYearSelection, Int] = ListMap(CurrentYear -> 52, CurrentYearMinus1 -> 50)
        val userAnswer = UserAnswers(userAnswersId)
          .set(NumberOfWeeksToClaimForPage, prepopAnswer)(NumberOfWeeksToClaimForPage.format).success.value
          .set(SelectTaxYearsToClaimForPage, Seq(CurrentYear, CurrentYearMinus1)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
        val request = FakeRequest(GET, testRoute)
        val result = route(application, request).value

        status(result) mustBe OK
        prepopulatedValue(result, CurrentYear.toTaxYear.startYear.toString) mustBe "52"
        prepopulatedValue(result, CurrentYearMinus1.toTaxYear.startYear.toString) mustBe "50"

        application.stop()
      }
    }
  }

  s"NumberOfWeeksToClaimForController POST" when {
    "user has not selected any years with week claims" must {
      "redirect to journey start as the user is page hopping" in {
        val userAnswer = UserAnswers(userAnswersId)
          .set(SelectTaxYearsToClaimForPage, Seq(CurrentYearMinus4)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
        val request = FakeRequest(POST, testRoute)
          .withFormUrlEncodedBody((CurrentYearMinus4.toTaxYear.startYear.toString, "30"))
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IndexController.start.url)

        application.stop()
      }
    }
    "user has selected current year" must {
      "return a redirect to confirm claim in weeks" in {
        val argCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(mockSessionService.set(argCaptor.capture())(any())).thenReturn(Future.successful(true))

        val userAnswer = UserAnswers(userAnswersId)
          .set(SelectTaxYearsToClaimForPage, Seq(CurrentYear)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswer))
          .overrides(bind[SessionService].toInstance(mockSessionService))
          .build()
        val request = FakeRequest(POST, testRoute)
          .withFormUrlEncodedBody((CurrentYear.toTaxYear.startYear.toString, "30"))
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ConfirmClaimInWeeksController.onPageLoad().url)
        argCaptor.getValue.get(NumberOfWeeksToClaimForPage)(NumberOfWeeksToClaimForPage.format) mustBe Some(ListMap(CurrentYear -> 30))

        application.stop()
      }
    }
    "user has selected previous year" must {
      "return an OK with the view" in {
        val argCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(mockSessionService.set(argCaptor.capture())(any())).thenReturn(Future.successful(true))

        val userAnswer = UserAnswers(userAnswersId)
          .set(SelectTaxYearsToClaimForPage, Seq(CurrentYearMinus1)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswer))
          .overrides(bind[SessionService].toInstance(mockSessionService))
          .build()
        val request = FakeRequest(POST, testRoute)
          .withFormUrlEncodedBody((CurrentYearMinus1.toTaxYear.startYear.toString, "50"))
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ConfirmClaimInWeeksController.onPageLoad().url)
        argCaptor.getValue.get(NumberOfWeeksToClaimForPage)(NumberOfWeeksToClaimForPage.format) mustBe Some(ListMap(CurrentYearMinus1 -> 50))

        application.stop()
      }
    }
    "user has selected multiple years with week based claims" must {
      "return an OK with the view" in {
        val argCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(mockSessionService.set(argCaptor.capture())(any())).thenReturn(Future.successful(true))

        val userAnswer = UserAnswers(userAnswersId)
          .set(SelectTaxYearsToClaimForPage, Seq(CurrentYear, CurrentYearMinus1)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswer))
          .overrides(bind[SessionService].toInstance(mockSessionService))
          .build()
        val request = FakeRequest(POST, testRoute)
          .withFormUrlEncodedBody((CurrentYear.toTaxYear.startYear.toString, "52"), (CurrentYearMinus1.toTaxYear.startYear.toString, "50"))
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ConfirmClaimInWeeksController.onPageLoad().url)
        argCaptor.getValue.get(NumberOfWeeksToClaimForPage)(NumberOfWeeksToClaimForPage.format) mustBe Some(ListMap(CurrentYear -> 52, CurrentYearMinus1 -> 50))

        application.stop()
      }
    }
  }
}
