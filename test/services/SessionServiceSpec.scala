/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors.EmployeeExpensesConnector
import models.UserAnswers
import org.mockito.ArgumentMatchers.{any, eq => eqm}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import pages.MergedJourneyFlag
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

class SessionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val mockSessionRepository: SessionRepository = mock[SessionRepository]
  val mockEmployeeExpensesConnector: EmployeeExpensesConnector = mock[EmployeeExpensesConnector]

  object TestService extends SessionService(
    mockSessionRepository,
    mockEmployeeExpensesConnector
  )(app.injector.instanceOf[ExecutionContext])

  val testId = "testId"

  def testUserAnswers(isMergedJourney: Boolean): UserAnswers = UserAnswers(
    testId,
    Json.obj(
      MergedJourneyFlag.toString -> isMergedJourney
    )
  )

  before {
    Mockito.reset(mockSessionRepository, mockEmployeeExpensesConnector)
  }

  "set" must {
    "return true when storing successfully" in {
      val testData = testUserAnswers(false)
      when(mockSessionRepository.set(eqm(testData))).thenReturn(Future.successful(true))

      await(TestService.set(testData)) mustBe true
      verify(mockEmployeeExpensesConnector, times(0)).updateMergedJourneySession(any())
    }

    "return true when storing successfully and call EE to refresh merged journey session if flag is set" in {
      val testData = testUserAnswers(true)
      when(mockSessionRepository.set(eqm(testData))).thenReturn(Future.successful(true))
      when(mockEmployeeExpensesConnector.updateMergedJourneySession(any())).thenReturn(Future.successful(true))

      await(TestService.set(testData)) mustBe true
      verify(mockEmployeeExpensesConnector, times(1)).updateMergedJourneySession(any())
    }
  }

  "get" must {
    "return data found in the repository" in {
      val testData = testUserAnswers(false)
      when(mockSessionRepository.get(eqm(testId))).thenReturn(Future.successful(Some(testData)))

      await(TestService.get(testId)) mustBe Some(testData)
    }
  }

  "updateTimeToLive" must {
    "return true on success for non merged journey" in {
      val testData = testUserAnswers(false)
      when(mockSessionRepository.get(eqm(testId))).thenReturn(Future.successful(Some(testData)))
      when(mockSessionRepository.set(eqm(testData))).thenReturn(Future.successful(true))

      await(TestService.updateTimeToLive(testId)) mustBe true
    }

    "return true on success for merged journey" in {
      val testData = testUserAnswers(true)
      when(mockSessionRepository.get(eqm(testId))).thenReturn(Future.successful(Some(testData)))
      when(mockSessionRepository.set(eqm(testData))).thenReturn(Future.successful(true))
      when(mockEmployeeExpensesConnector.updateMergedJourneySession(any())).thenReturn(Future.successful(true))

      await(TestService.updateTimeToLive(testId)) mustBe true
    }
  }
}
