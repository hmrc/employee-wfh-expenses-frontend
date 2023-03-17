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

import connectors.TaiConnector
import models.{Expenses, IABDExpense}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import utils.RateLimiting

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IABDServiceImpl @Inject()(
                                 taiConnector: TaiConnector,
                                 @Named("IABD GET") rateLimiter: RateLimiting
                               ) extends IABDService with Logging {

  def alreadyClaimed(nino: String, year: Int)(implicit hc:HeaderCarrier):Future[Option[Expenses]] = {

    { for {
      otherExpenses   <- rateLimiter.withToken(() => taiConnector.getOtherExpensesData(nino, year))
      otherRateAmount = otherExpenses.map(_.grossAmount).sum
      jobExpenses     <-
        if (otherRateAmount==0) {
          rateLimiter.withToken(() => taiConnector.getJobExpensesData(nino, year))
        } else {
          Future.successful(Seq[IABDExpense]())
        }
      jobRateAmount             = jobExpenses.map(_.grossAmount).sum
      wasJobRateExpensesChecked = if (otherRateAmount == 0) true else false
    } yield
      if (otherRateAmount > 0 || jobRateAmount > 0) {
        Some(Expenses(year, otherExpenses, jobExpenses, wasJobRateExpensesChecked))
      } else {
        None
      }
    }
  }
}

trait IABDService {
  def alreadyClaimed(nino: String, year: Int)(implicit hc:HeaderCarrier):Future[Option[Expenses]]
}
