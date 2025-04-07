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

import connectors.EmployeeExpensesConnector
import models.UserAnswers
import play.api.Logging
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionService @Inject() (
    sessionRepository: SessionRepository,
    employeeExpensesConnector: EmployeeExpensesConnector
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def set(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Boolean] =
    for {
      repoResponse <- sessionRepository.set(userAnswers)
      mergedJourneyRefreshed <-
        if (userAnswers.isMergedJourney) employeeExpensesConnector.updateMergedJourneySession(hc)
        else Future.successful(true)
      _ = if (!mergedJourneyRefreshed) logger.warn("EE merged journey session could not be refreshed")
    } yield repoResponse

  def get(id: String): Future[Option[UserAnswers]] = sessionRepository.get(id)

  def updateTimeToLive(id: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    sessionRepository.get(id).flatMap {
      case Some(userAnswers) => set(userAnswers)
      case _                 => Future.successful(false)
    }

}
