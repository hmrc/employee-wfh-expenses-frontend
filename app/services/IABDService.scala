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

import config.FrontendAppConfig
import connectors.TaiConnector
import models.auditing.AuditEventType.AlreadyClaimedExpenses
import models.{Expenses, IABDExpense}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.RateLimiting
import utils.TaxYearDates._

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IABDServiceImpl @Inject()(
                                 taiConnector: TaiConnector,
                                 auditConnector: AuditConnector,
                                 appConfig: FrontendAppConfig,
                                 @Named("IABD GET") rateLimiter: RateLimiting
                               )(implicit executionContext: ExecutionContext) extends IABDService with Logging {

  def alreadyClaimed(nino: String, year: Int)(implicit hc: HeaderCarrier): Future[Option[Expenses]] = {
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

  def getAlreadyClaimedStatusForAllYears(nino: String)(implicit hc: HeaderCarrier): Future[(Option[Expenses], Option[Expenses], Option[Expenses], Option[Expenses], Option[Expenses])] = {
    for {
      alreadyClaimed2020 <- alreadyClaimed(nino, YEAR_2020)
      alreadyClaimed2021 <- alreadyClaimed(nino, YEAR_2021)
      alreadyClaimed2022 <- alreadyClaimed(nino, YEAR_2022)
      alreadyClaimed2023 <- alreadyClaimed(nino, YEAR_2023)
      alreadyClaimed2024 <- alreadyClaimed(nino, YEAR_2024)
    } yield {
      (alreadyClaimed2020, alreadyClaimed2021, alreadyClaimed2022, alreadyClaimed2023, alreadyClaimed2024)
    }
  }

  def allYearsClaimed(nino: String,
                      yearsClaimed: (Option[Expenses], Option[Expenses], Option[Expenses], Option[Expenses], Option[Expenses]),
                      audit: Boolean = true)(implicit hc: HeaderCarrier): Boolean = {
    yearsClaimed match {
      case (Some(claimed2020), Some(claimed2021), Some(claimed2022), Some(claimed2023), Some(claimed2024)) =>
        logger.info(s"[IABDService][hasClaimedForAllYears] Detected already claimed for" +
          s" $YEAR_2020, $YEAR_2021, $YEAR_2022, $YEAR_2023 and $YEAR_2024; redirecting to P87 digital form")
        if (audit) {
          auditAlreadyClaimed(nino, YEAR_2020, claimed2020.otherExpenses, claimed2020.jobExpenses, claimed2020.wasJobRateExpensesChecked)
          auditAlreadyClaimed(nino, YEAR_2021, claimed2021.otherExpenses, claimed2021.jobExpenses, claimed2021.wasJobRateExpensesChecked)
          auditAlreadyClaimed(nino, YEAR_2022, claimed2022.otherExpenses, claimed2022.jobExpenses, claimed2022.wasJobRateExpensesChecked)
          auditAlreadyClaimed(nino, YEAR_2023, claimed2023.otherExpenses, claimed2023.jobExpenses, claimed2023.wasJobRateExpensesChecked)
          auditAlreadyClaimed(nino, YEAR_2024, claimed2024.otherExpenses, claimed2024.jobExpenses, claimed2024.wasJobRateExpensesChecked)
        }
        true
      case _ => false
    }
  }

  def claimedAllYearsStatus(nino: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    getAlreadyClaimedStatusForAllYears(nino).map { claimedYears =>
      if (allYearsClaimed(nino, claimedYears, audit = false)) true else false
    }.recoverWith {
      case ex: Exception =>
        val message = s"[IABDService][claimedAllYearsStatus] TAI lookup failed with: ${ex.getMessage}"
        logger.error(message)
        Future.failed(ex)
    }
  }

  private def auditAlreadyClaimed(
                                   nino: String,
                                   year: Int,
                                   otherExpenses: Seq[IABDExpense],
                                   jobExpenses: Seq[IABDExpense],
                                   wasJobRateExpensesChecked: Boolean
                                 )(implicit hc: HeaderCarrier,
                                   executionContext: ExecutionContext): Unit = {

    val json = if (wasJobRateExpensesChecked) {
      Json.obj(fields =
        "nino" -> nino,
        s"taxYear" -> year,
        s"iabd-${appConfig.otherExpensesId}" -> otherExpenses,
        s"iabd-${appConfig.jobExpenseId}" -> jobExpenses
      )
    } else {
      Json.obj(fields =
        "nino" -> nino,
        s"taxYear" -> year,
        s"iabd-${appConfig.otherExpensesId}" -> otherExpenses,
        s"iabd-${appConfig.jobExpenseId}" -> "NOT CHECKED"
      )
    }

    auditConnector.sendExplicitAudit(AlreadyClaimedExpenses.toString, json)
  }
}


trait IABDService {
  def alreadyClaimed(nino: String, year: Int)(implicit hc: HeaderCarrier): Future[Option[Expenses]]
  def getAlreadyClaimedStatusForAllYears(nino: String)(implicit hc: HeaderCarrier): Future[(Option[Expenses], Option[Expenses], Option[Expenses], Option[Expenses], Option[Expenses])]
  def allYearsClaimed(nino: String, yearsClaimed: (Option[Expenses], Option[Expenses], Option[Expenses], Option[Expenses], Option[Expenses]), audit: Boolean = true)(implicit hc: HeaderCarrier): Boolean
  def claimedAllYearsStatus(nino: String)(implicit hc: HeaderCarrier): Future[Boolean]
}
