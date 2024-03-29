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

package services

import base.SpecBase
import com.digitaltangible.ratelimit.RateLimiter
import config.{FrontendAppConfig, RateLimitConfig}
import connectors.TaiConnector
import models.{Expenses, IABDExpense}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, when}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.TooManyRequestException
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.RateLimiting

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IABDServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val mockTaiConnector: TaiConnector = mock[TaiConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  //noinspection ScalaStyle
  def throttler(enabled: Boolean) = new RateLimiting(RateLimitConfig(10, 10, enabled), "too many requests")

  class Setup(throttler: RateLimiting) {
    val serviceUnderTest = new IABDServiceImpl(mockTaiConnector, mockAuditConnector, mockAppConfig, throttler)
  }

  before {
    Mockito.reset(mockTaiConnector)
  }

  val testYear2023 = 2023
  val testYear2022 = 2022
  val testYear2021 = 2021
  val testYear2020 = 2020
  val testOtherExpensesAmount = 123
  val testJobExpensesAmount = 321

  "alreadyClaimed" should {
    "return None" when {
      "there is no job or other expenses" in new Setup(throttler(true)) {
        when(mockTaiConnector.getOtherExpensesData(fakeNino, testYear2023)).thenReturn(Future(Seq.empty))
        when(mockTaiConnector.getJobExpensesData(fakeNino, testYear2023)).thenReturn(Future(Seq.empty))

        val maybeExpenses: Option[Expenses] = await(serviceUnderTest.alreadyClaimed(fakeNino, testYear2023))
        maybeExpenses mustBe None

        Mockito.verify(mockTaiConnector, times(1)).getOtherExpensesData(fakeNino, testYear2023)
        Mockito.verify(mockTaiConnector, times(1)).getJobExpensesData(fakeNino, testYear2023)
      }
    }

    "return Some other expenses" when {
      "tai returns other expense details" in new Setup(throttler(true)) {
        val otherExpenses: Seq[IABDExpense] = Seq(IABDExpense(testOtherExpensesAmount))

        when(mockTaiConnector.getOtherExpensesData(fakeNino, testYear2023)).thenReturn(Future(otherExpenses))

        val maybeExpenses: Option[Expenses] = await(serviceUnderTest.alreadyClaimed(fakeNino, testYear2023))
        maybeExpenses mustBe Some(Expenses(testYear2023, otherExpenses, Seq.empty, wasJobRateExpensesChecked = false))

        Mockito.verify(mockTaiConnector, times(1)).getOtherExpensesData(fakeNino, testYear2023)
        Mockito.verify(mockTaiConnector, times(0)).getJobExpensesData(fakeNino, testYear2023)
      }
    }

    "return Some job expenses" when {
      "tai returns job expense details" in new Setup(throttler(true)) {
        val otherExpenses: Seq[Nothing] = Seq.empty
        val jobExpenses: Seq[IABDExpense] = Seq(IABDExpense(testJobExpensesAmount))

        when(mockTaiConnector.getOtherExpensesData(fakeNino, testYear2023)).thenReturn(Future(otherExpenses))
        when(mockTaiConnector.getJobExpensesData(fakeNino, testYear2023)).thenReturn(Future(jobExpenses))

        val maybeExpenses: Option[Expenses] = await(serviceUnderTest.alreadyClaimed(fakeNino, testYear2023))
        maybeExpenses mustBe Some(Expenses(testYear2023, Seq.empty, jobExpenses, wasJobRateExpensesChecked = true))

        Mockito.verify(mockTaiConnector, times(1)).getOtherExpensesData(fakeNino, testYear2023)
        Mockito.verify(mockTaiConnector, times(1)).getJobExpensesData(fakeNino, testYear2023)
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

        when(mockTaiConnector.getOtherExpensesData(fakeNino, testYear2023)).thenReturn(Future(Seq.empty))
        when(mockTaiConnector.getJobExpensesData(fakeNino, testYear2023)).thenReturn(Future(Seq.empty))

        intercept[TooManyRequestException] {
          await(serviceUnderTest.alreadyClaimed(fakeNino, testYear2023))
        }

        Mockito.verify(mockTaiConnector, times(0)).getOtherExpensesData(fakeNino, testYear2023)
        Mockito.verify(mockTaiConnector, times(0)).getJobExpensesData(fakeNino, testYear2023)
      }
    }

    "NOT throw a TooManyRequestException" when {

      val mockThrottler = mock[RateLimiting]

      "the throttle is disabled" in new Setup(mockThrottler) {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.bucket).thenReturn(new FailToGetTokenRateLimiter)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockTaiConnector.getOtherExpensesData(fakeNino, testYear2023)).thenReturn(Future(Seq.empty))
        when(mockTaiConnector.getJobExpensesData(fakeNino, testYear2023)).thenReturn(Future(Seq.empty))

        await(serviceUnderTest.alreadyClaimed(fakeNino, testYear2023))

        Mockito.verify(mockTaiConnector, times(1)).getOtherExpensesData(fakeNino, testYear2023)
        Mockito.verify(mockTaiConnector, times(1)).getJobExpensesData(fakeNino, testYear2023)
      }
    }
  }

  "getAlreadyClaimedStatusForAllYears" should {
    "return expenses for 2023 only when already claimed for CTY" in new Setup(throttler(false)) {
      val jobExpenses: Seq[IABDExpense] = Seq(IABDExpense(testJobExpensesAmount))
      val noExpenses: Seq[Nothing] = Seq.empty

      when(mockTaiConnector.getOtherExpensesData(fakeNino, testYear2023)).thenReturn(Future(noExpenses))
      when(mockTaiConnector.getJobExpensesData(fakeNino, testYear2023)).thenReturn(Future(jobExpenses))

      for(year <- Seq(testYear2022, testYear2021, testYear2020)) {
        when(mockTaiConnector.getOtherExpensesData(fakeNino, year)).thenReturn(Future(noExpenses))
        when(mockTaiConnector.getJobExpensesData(fakeNino, year)).thenReturn(Future(noExpenses))
      }

      val expenses: (None.type, None.type, None.type, Some[Expenses]) = (
        None,
        None,
        None,
        Some(Expenses(testYear2023, noExpenses, jobExpenses, wasJobRateExpensesChecked = true))
      )

      await(serviceUnderTest.getAlreadyClaimedStatusForAllYears(fakeNino)) mustBe expenses
    }
  }

  "allYearsClaimed" should {
    "return true when data provided shows all years have been claimed" in new Setup(throttler(true)) {
      val jobExpenses: Seq[IABDExpense] = Seq(IABDExpense(testJobExpensesAmount))
      val otherExpenses: Seq[IABDExpense] = Seq(IABDExpense(testOtherExpensesAmount))

      val expenses: (Option[Expenses], Option[Expenses], Option[Expenses], Option[Expenses]) = (
        Some(Expenses(testYear2020, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true)),
        Some(Expenses(testYear2021, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true)),
        Some(Expenses(testYear2022, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true)),
        Some(Expenses(testYear2023, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true))
      )

      serviceUnderTest.allYearsClaimed(fakeNino, expenses) mustBe true
    }

    "return false when data provided shows all years have NOT been claimed" in new Setup(throttler(true)) {
      val jobExpenses: Seq[IABDExpense] = Seq(IABDExpense(testJobExpensesAmount))
      val otherExpenses: Seq[IABDExpense] = Seq(IABDExpense(testOtherExpensesAmount))

      val expenses: (Option[Expenses], Option[Expenses], Option[Expenses], Option[Expenses]) = (
        None,
        Some(Expenses(testYear2021, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true)),
        Some(Expenses(testYear2022, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true)),
        Some(Expenses(testYear2023, otherExpenses, jobExpenses, wasJobRateExpensesChecked = true))
      )

      serviceUnderTest.allYearsClaimed(fakeNino, expenses) mustBe false
    }
  }

  "claimedAllYearsStatus" should {
    "return a value of true if user has claimed for all years" in new Setup(throttler(true)) {
      val jobExpenses: Seq[IABDExpense] = Seq(IABDExpense(testJobExpensesAmount))
      val otherExpenses: Seq[IABDExpense] = Seq(IABDExpense(testOtherExpensesAmount))

      for (year <- Seq(testYear2023, testYear2022, testYear2021, testYear2020)) {
        when(mockTaiConnector.getOtherExpensesData(fakeNino, year)).thenReturn(Future(otherExpenses))
        when(mockTaiConnector.getJobExpensesData(fakeNino, year)).thenReturn(Future(jobExpenses))
      }

      await(serviceUnderTest.claimedAllYearsStatus(fakeNino)) mustBe true
    }

    "return a value of false if user has NOT claimed for all years" in new Setup(throttler(true)) {
      for (year <- Seq(testYear2023, testYear2022, testYear2021, testYear2020)) {
        when(mockTaiConnector.getOtherExpensesData(fakeNino, year)).thenReturn(Future(Seq.empty))
        when(mockTaiConnector.getJobExpensesData(fakeNino, year)).thenReturn(Future(Seq.empty))
      }

      await(serviceUnderTest.claimedAllYearsStatus(fakeNino)) mustBe false
    }
  }
}
