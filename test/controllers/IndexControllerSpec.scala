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
import models.{Expenses, IABDExpense}
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.IABDService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IndexControllerSpec extends SpecBase with BeforeAndAfter {

  val testOtherExpensesAmount = 123
  val testJobExpensesAmount = 321
  val testYear = 2020

  val mockIABDService = mock[IABDService]

  before {
    Mockito.reset(mockIABDService)
  }

  "Index Controller" must {
    val otherExpenses = Seq(IABDExpense(testOtherExpensesAmount))
    val jobExpenses = Seq(IABDExpense(testJobExpensesAmount))

    "redirect to the Disclaimer page for a GET" when {

      val tests = Seq(
        ("already claimed other expenses for 2020", Expenses(testYear, otherExpenses, Seq.empty, false)),
        ("already claimed job expenses for 2020", Expenses(testYear, Seq.empty, jobExpenses, true)),
        ("already claimed other and job expenses for 2020", Expenses(testYear, otherExpenses, jobExpenses, true))
      )

      for ((desc, expenses) <- tests) {
        s"$desc" in {
          when(mockIABDService.alreadyClaimed(any(), any())(any())).thenReturn(Future(Some(expenses)))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[IABDService].toInstance(mockIABDService))
            .build()

          val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustEqual Some("/employee-working-from-home-expenses/disclaimer")

          application.stop()
        }
      }
    }

  }
}
