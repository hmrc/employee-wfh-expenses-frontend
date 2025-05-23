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
import config.FrontendAppConfig
import models.requests.OptionalDataRequest
import models.{Expenses, IABDExpense}
import navigation.Navigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import pages.MergedJourneyFlag
import play.api.inject.bind
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{IABDService, SessionService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IndexControllerSpec extends SpecBase with BeforeAndAfter {

  val testOtherExpensesAmount = 123
  val testJobExpensesAmount   = 321
  val testYear2020            = 2020
  val testYear2021            = 2021
  val testYear2022            = 2022
  val testYear2023            = 2023
  val testYear2024            = 2024

  val mockIABDService: IABDService       = mock[IABDService]
  val mockNavigator: Navigator           = mock[Navigator]
  val mockSessionService: SessionService = mock[SessionService]
  val mockAppConfig: FrontendAppConfig   = mock[FrontendAppConfig]

  implicit val defaultOptionalDataRequest: OptionalDataRequest[AnyContent] = OptionalDataRequest(
    FakeRequest("GET", "?eligibilityCheckerSessionId=qqq"),
    "XXX",
    None,
    "XX"
  )

  before {
    Mockito.reset(mockIABDService)
  }

  "onPageLoad" must {
    val otherExpenses = Seq(IABDExpense(testOtherExpensesAmount))
    val jobExpenses   = Seq(IABDExpense(testJobExpensesAmount))

    "redirect to the TaxYearSelection page for a GET" when {
      "not claimed expenses for any years" in {
        val expenses = Nil
        when(mockIABDService.getAlreadyClaimedStatusForAllYears(any())(any())).thenReturn(Future(expenses))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[IABDService].toInstance(mockIABDService))
          .build()

        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result = route(application, request).value
        redirectLocation(result).get mustBe routes.SelectTaxYearsToClaimForController.onPageLoad().url

        application.stop()
      }

      "claimed expenses for some years" in {
        val expenses = Seq(
          Expenses(testYear2023, otherExpenses, Seq.empty, wasJobRateExpensesChecked = false),
          Expenses(testYear2021, Seq.empty, jobExpenses, wasJobRateExpensesChecked = true)
        )

        when(mockIABDService.getAlreadyClaimedStatusForAllYears(any())(any())).thenReturn(Future(expenses))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[IABDService].toInstance(mockIABDService))
          .build()

        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result = route(application, request).value
        redirectLocation(result).get mustBe routes.SelectTaxYearsToClaimForController.onPageLoad().url

        application.stop()
      }
    }

    "redirect to P87SUB form" when {
      "claimed expenses for all years" in {
        val expenses = Seq(
          Expenses(testYear2024, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true),
          Expenses(testYear2023, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true),
          Expenses(testYear2022, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true),
          Expenses(testYear2021, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true),
          Expenses(testYear2020, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true)
        )

        when(mockIABDService.getAlreadyClaimedStatusForAllYears(any())(any())).thenReturn(Future(expenses))
        when(mockIABDService.allYearsClaimed(any(), any(), any())(any())).thenReturn(true)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[IABDService].toInstance(mockIABDService))
          .build()

        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result = route(application, request).value
        redirectLocation(result).get.contains(
          "digital-forms/form/tax-relief-for-expenses-of-employment/draft/guide"
        ) mustBe true

        application.stop()
      }
    }
  }

  "start" must {
    "redirect to index with merged journey flag if user is on a merged journey" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers.set(MergedJourneyFlag, true).get))
        .overrides(bind[SessionService].toInstance(mockSessionService))
        .build()

      val request = FakeRequest(GET, routes.IndexController.start.url)
      val result  = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.IndexController.onPageLoad(true).url

      application.stop()
    }
    "redirect to index if there are no user answers" in {
      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[SessionService].toInstance(mockSessionService))
        .build()

      val request = FakeRequest(GET, routes.IndexController.start.url)
      val result  = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url

      application.stop()
    }
  }

}
