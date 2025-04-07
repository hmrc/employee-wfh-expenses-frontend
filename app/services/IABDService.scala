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
import models.TaxYearSelection._
import models.auditing.AuditEventType.AlreadyClaimedExpenses
import models.{Expenses, IABDExpense}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IABDService @Inject() (taiConnector: TaiConnector, auditConnector: AuditConnector, appConfig: FrontendAppConfig)(
    implicit executionContext: ExecutionContext
) extends Logging {

  def alreadyClaimed(nino: String, year: Int)(implicit hc: HeaderCarrier): Future[Option[Expenses]] =
    for {
      otherExpenses <- taiConnector.getOtherExpensesData(nino, year)
      otherRateAmount = otherExpenses.map(_.grossAmount).sum
      jobExpenses <-
        if (otherRateAmount == 0) {
          taiConnector.getJobExpensesData(nino, year)
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

  def getAlreadyClaimedStatusForAllYears(nino: String)(implicit hc: HeaderCarrier): Future[Seq[Expenses]] =
    for {
      alreadyClaimedCy       <- alreadyClaimed(nino, CurrentYear.toTaxYear.startYear)
      alreadyClaimedCyMinus1 <- alreadyClaimed(nino, CurrentYearMinus1.toTaxYear.startYear)
      alreadyClaimedCyMinus2 <- alreadyClaimed(nino, CurrentYearMinus2.toTaxYear.startYear)
      alreadyClaimedCyMinus3 <- alreadyClaimed(nino, CurrentYearMinus3.toTaxYear.startYear)
      alreadyClaimedCyMinus4 <- alreadyClaimed(nino, CurrentYearMinus4.toTaxYear.startYear)
    } yield Seq(
      alreadyClaimedCy,
      alreadyClaimedCyMinus1,
      alreadyClaimedCyMinus2,
      alreadyClaimedCyMinus3,
      alreadyClaimedCyMinus4
    ).flatten

  def allYearsClaimed(nino: String, claimedYears: Seq[Expenses], audit: Boolean = true)(
      implicit hc: HeaderCarrier
  ): Boolean =
    claimedYears match {
      case list if list.size == 5 =>
        logger.info(
          s"[IABDService][allYearsClaimed] Detected already claimed for" +
            s" all five tax years, redirecting to P87 digital form"
        )
        if (audit) {
          claimedYears.foreach(expenses =>
            auditAlreadyClaimed(
              nino,
              expenses.year,
              expenses.otherExpenses,
              expenses.jobExpenses,
              expenses.wasJobRateExpensesChecked
            )
          )
        }
        true
      case _ => false
    }

  def claimedAllYearsStatus(nino: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    getAlreadyClaimedStatusForAllYears(nino)
      .map(claimedYears => allYearsClaimed(nino, claimedYears, audit = false))
      .recoverWith { case ex: Exception =>
        val message = s"[IABDService][claimedAllYearsStatus] TAI lookup failed with: ${ex.getMessage}"
        logger.error(message)
        Future.failed(ex)
      }

  private def auditAlreadyClaimed(
      nino: String,
      year: Int,
      otherExpenses: Seq[IABDExpense],
      jobExpenses: Seq[IABDExpense],
      wasJobRateExpensesChecked: Boolean
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Unit = {

    val json = if (wasJobRateExpensesChecked) {
      Json.obj(
        fields = "nino" -> nino,
        s"taxYear"                           -> year,
        s"iabd-${appConfig.otherExpensesId}" -> otherExpenses,
        s"iabd-${appConfig.jobExpenseId}"    -> jobExpenses
      )
    } else {
      Json.obj(
        fields = "nino" -> nino,
        s"taxYear"                           -> year,
        s"iabd-${appConfig.otherExpensesId}" -> otherExpenses,
        s"iabd-${appConfig.jobExpenseId}"    -> "NOT CHECKED"
      )
    }

    auditConnector.sendExplicitAudit(AlreadyClaimedExpenses.toString, json)
  }

}
