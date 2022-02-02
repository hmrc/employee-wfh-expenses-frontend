/*
 * Copyright 2022 HM Revenue & Customs
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
import models.requests.OptionalDataRequest
import models.{Expenses, IABDExpense}
import navigation.Navigator
import org.junit.Assert.{assertEquals, assertTrue}
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, Call, Headers, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.IABDService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}


case class TaiLookupHandlerImpl(override val iabdService: IABDService,
                                override val navigator: Navigator,
                                override val sessionRepository: SessionRepository)
  extends TaiLookupHandler

class IndexControllerSpec extends SpecBase with BeforeAndAfter {

  val testOtherExpensesAmount = 123
  val testJobExpensesAmount = 321
  val testYear = 2020

  val mockIABDService = mock[IABDService]
  val mockNavigator = mock[Navigator]
  val mockSessionRepository = mock[SessionRepository]

  val taiIndexLookupServiceUnderTest = TaiLookupHandlerImpl(mockIABDService, mockNavigator, mockSessionRepository)

  implicit val defaultOptionalDataRequest: OptionalDataRequest[AnyContent] = OptionalDataRequest(
    FakeRequest("GET", "?eligibilityCheckerSessionId=qqq"), "XXX", None, "XX", None)

  val taiLookupSuccessHandler = (a: Boolean, b: Boolean, c: Boolean) => Ok

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

          val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustEqual Some("/employee-working-from-home-expenses")

          application.stop()
        }
      }
    }

    "TaiIndexLookupService should handle error in tai calls" when {
      when(mockIABDService.alreadyClaimed(any(), any())(any())).thenReturn(
        Future.failed(new RuntimeException("Error123")))

      val eventualResult = taiIndexLookupServiceUnderTest.handlePageRequest(taiLookupSuccessHandler)

      val futureResult = Await.ready(eventualResult, 1 second)
      assertTrue(futureResult.value.get.failed.get.getMessage.contains("Error123"))
    }

    "TaiIndexLookupService should handle no errors in tai calls" when {
      when(mockIABDService.alreadyClaimed(any(), any())(any())).thenReturn(
        Future.successful(Some(Expenses(testYear, otherExpenses, Seq.empty, false))))

      val eventualResult = taiIndexLookupServiceUnderTest.handlePageRequest(taiLookupSuccessHandler)

      val futureResult = Await.ready(eventualResult, 1 second)
      assertTrue(futureResult.value.get.get.header.status == 200)
    }

    "TaiIndexLookupService should build success result correctly" when {

      when(mockNavigator.nextPage(any(), any())).thenReturn(Call("method-1", "url-123", "fragment-string"))

      val result = taiIndexLookupServiceUnderTest.taiLookupSuccessHandler(true,
        true, true)

      verify(mockNavigator, times(1)).nextPage(any(), any())

      assertEquals(result.header.headers.get("Location").get,
        "url-123#fragment-string")

    }
  }
}
