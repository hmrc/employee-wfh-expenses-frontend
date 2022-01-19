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

package services

import base.SpecBase
import com.digitaltangible.playguard.RateLimiter
import config.RateLimitConfig
import connectors.TaiConnector
import models.{Expenses, IABDExpense}
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, when}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.TooManyRequestException
import utils.RateLimiting

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IABDServiceImplSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val mockTaiConnector = mock[TaiConnector]


  //noinspection ScalaStyle
  val anEnabledThrottler = new RateLimiting(RateLimitConfig(10, 10, enabled = true), "too many requests")

  class Setup(throttler: RateLimiting) {
    val serviceUnderTest = new IABDServiceImpl(mockTaiConnector, throttler)
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
      "there is no job or other expenses" in new Setup(anEnabledThrottler) {
        when(mockTaiConnector.getOtherExpensesData(testNino,testYear)).thenReturn(Future(Seq.empty))
        when(mockTaiConnector.getJobExpensesData(testNino,testYear)).thenReturn(Future(Seq.empty))

        val maybeExpenses: Option[Expenses] = await(serviceUnderTest.alreadyClaimed(testNino, testYear))
        maybeExpenses mustBe None

        Mockito.verify(mockTaiConnector, times(1)).getOtherExpensesData(testNino,testYear)
        Mockito.verify(mockTaiConnector, times(1)).getJobExpensesData(testNino,testYear)
      }
    }

    "return Some other expenses" when {
      "tai returns other expense details" in new Setup(anEnabledThrottler) {
        val otherExpenses = Seq(IABDExpense(testOtherExpensesAmount))

        when(mockTaiConnector.getOtherExpensesData(testNino,testYear)).thenReturn(Future(otherExpenses))

        val maybeExpenses: Option[Expenses] = await(serviceUnderTest.alreadyClaimed(testNino, testYear))
        maybeExpenses mustBe Some(Expenses(testYear, otherExpenses, Seq.empty, wasJobRateExpensesChecked = false))

        Mockito.verify(mockTaiConnector, times(1)).getOtherExpensesData(testNino,testYear)
        Mockito.verify(mockTaiConnector, times(0)).getJobExpensesData(testNino,testYear)
      }
    }

    "return Some job expenses" when {
      "tai returns job expense details" in new Setup(anEnabledThrottler) {
        val otherExpenses = Seq.empty
        val jobExpenses = Seq(IABDExpense(testJobExpensesAmount))

        when(mockTaiConnector.getOtherExpensesData(testNino,testYear)).thenReturn(Future(otherExpenses))
        when(mockTaiConnector.getJobExpensesData(testNino,testYear)).thenReturn(Future(jobExpenses))

        val maybeExpenses: Option[Expenses] = await(serviceUnderTest.alreadyClaimed(testNino, testYear))
        maybeExpenses mustBe Some(Expenses(testYear, Seq.empty, jobExpenses, wasJobRateExpensesChecked = true))

        Mockito.verify(mockTaiConnector, times(1)).getOtherExpensesData(testNino,testYear)
        Mockito.verify(mockTaiConnector, times(1)).getJobExpensesData(testNino,testYear)
      }
    }

    class FailToGetTokenRateLimiter extends RateLimiter(1,1) {
      override def consumeAndCheck(key: Any): Boolean = false
    }

    "throw a TooManyRequestException exception" when {

      val mockThrottler = mock[RateLimiting]

      "there are no tokens left in the bucket" in new Setup(mockThrottler) {
        when(mockThrottler.enabled).thenReturn(true)
        when(mockThrottler.bucket).thenReturn(new FailToGetTokenRateLimiter)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockTaiConnector.getOtherExpensesData(testNino,testYear)).thenReturn(Future(Seq.empty))
        when(mockTaiConnector.getJobExpensesData(testNino,testYear)).thenReturn(Future(Seq.empty))

        intercept[TooManyRequestException] {
          await(serviceUnderTest.alreadyClaimed(testNino, testYear))
        }

        Mockito.verify(mockTaiConnector, times(0)).getOtherExpensesData(testNino,testYear)
        Mockito.verify(mockTaiConnector, times(0)).getJobExpensesData(testNino,testYear)
      }
    }

    "NOT throw a TooManyRequestException" when {

      val mockThrottler = mock[RateLimiting]

      "the throttle is disabled" in new Setup(mockThrottler) {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.bucket).thenReturn(new FailToGetTokenRateLimiter)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockTaiConnector.getOtherExpensesData(testNino,testYear)).thenReturn(Future(Seq.empty))
        when(mockTaiConnector.getJobExpensesData(testNino,testYear)).thenReturn(Future(Seq.empty))

        await(serviceUnderTest.alreadyClaimed(testNino, testYear))

        Mockito.verify(mockTaiConnector, times(1)).getOtherExpensesData(testNino,testYear)
        Mockito.verify(mockTaiConnector, times(1)).getJobExpensesData(testNino,testYear)
      }
    }
  }
}
