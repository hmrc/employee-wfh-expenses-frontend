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

package services

import base.SpecBase
import connectors.TaiConnector
import models.{Expenses, IABDExpense}
import org.mockito.Mockito
import org.mockito.Mockito.{times, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.Matchers.{convertToAnyShouldWrapper, _}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IABDServiceImplSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val mockTaiConnector = mock[TaiConnector]

  class Setup {
    val serviceUnderTest = new IABDServiceImpl(mockTaiConnector)
  }

  before {
    Mockito.reset(mockTaiConnector)
  }

  val testNino = "AB123456A"
  val testYear = 2021
  val testOtherExpensesAmount = 123
  val testJobExpensesAmount = 321

  "alreadyClaimed" should {
    "return None" when {
      "there is no job or other expenses" in new Setup {
        when(mockTaiConnector.getOtherExpensesData(testNino,testYear)).thenReturn(Future(Seq.empty))
        when(mockTaiConnector.getJobExpensesData(testNino,testYear)).thenReturn(Future(Seq.empty))

        val maybeExpenses: Option[Expenses] = await(serviceUnderTest.alreadyClaimed(testNino, testYear))
        maybeExpenses shouldBe None

        Mockito.verify(mockTaiConnector, times(1)).getOtherExpensesData(testNino,testYear)
        Mockito.verify(mockTaiConnector, times(1)).getJobExpensesData(testNino,testYear)
      }
    }

    "return Some other expenses" when {
      "tai returns other expense details" in new Setup {
        val otherExpenses = Seq(IABDExpense(testOtherExpensesAmount))

        when(mockTaiConnector.getOtherExpensesData(testNino,testYear)).thenReturn(Future(otherExpenses))

        val maybeExpenses: Option[Expenses] = await(serviceUnderTest.alreadyClaimed(testNino, testYear))
        maybeExpenses shouldBe Some(Expenses(testYear, otherExpenses, Seq.empty, wasJobRateExpensesChecked = false))

        Mockito.verify(mockTaiConnector, times(1)).getOtherExpensesData(testNino,testYear)
        Mockito.verify(mockTaiConnector, times(0)).getJobExpensesData(testNino,testYear)
      }
    }

    "return Some job expenses" when {
      "tai returns job expense details" in new Setup {
        val otherExpenses = Seq.empty
        val jobExpenses = Seq(IABDExpense(testJobExpensesAmount))

        when(mockTaiConnector.getOtherExpensesData(testNino,testYear)).thenReturn(Future(otherExpenses))
        when(mockTaiConnector.getJobExpensesData(testNino,testYear)).thenReturn(Future(jobExpenses))

        val maybeExpenses: Option[Expenses] = await(serviceUnderTest.alreadyClaimed(testNino, testYear))
        maybeExpenses shouldBe Some(Expenses(testYear, Seq.empty, jobExpenses, wasJobRateExpensesChecked = true))

        Mockito.verify(mockTaiConnector, times(1)).getOtherExpensesData(testNino,testYear)
        Mockito.verify(mockTaiConnector, times(1)).getJobExpensesData(testNino,testYear)
      }
    }

  }
}