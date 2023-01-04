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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import models.IABDExpense
import models.auditing.AuditEventType.AlreadyClaimedExpenses
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.IABDServiceImpl
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.TaxYearDates.{YEAR_2020, YEAR_2021, YEAR_2022}

import scala.concurrent.{ExecutionContext, Future}


class CheckAlreadyClaimedActionImpl @Inject()(
                                               iabdService: IABDServiceImpl,
                                               appConfig: FrontendAppConfig,
                                               auditConnector: AuditConnector
                                                 )(implicit val executionContext: ExecutionContext) extends CheckAlreadyClaimedAction with Logging {

  override protected def filter[A](request: IdentifierRequest[A]): Future[Option[Result]] = {

    implicit val hc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    for {
      alreadyClaimed2020 <- iabdService.alreadyClaimed(request.nino, YEAR_2020)
      alreadyClaimed2021 <- iabdService.alreadyClaimed(request.nino, YEAR_2021)
      alreadyClaimed2022 <- iabdService.alreadyClaimed(request.nino, YEAR_2022)
    } yield {
      (alreadyClaimed2020, alreadyClaimed2021, alreadyClaimed2022) match {
        case (Some(claimed2020), Some(claimed2021), Some(claimed2022)) =>
          logger.info(s"[CheckAlreadyClaimedAction][filter] Detected already claimed for $YEAR_2020, $YEAR_2021 and $YEAR_2022; redirecting to P87 digital form")
          auditAlreadyClaimed(request.nino, request.saUtr, YEAR_2020, claimed2020.otherExpenses, claimed2020.jobExpenses, claimed2020.wasJobRateExpensesChecked)
          auditAlreadyClaimed(request.nino, request.saUtr, YEAR_2021, claimed2021.otherExpenses, claimed2021.jobExpenses, claimed2021.wasJobRateExpensesChecked)
          auditAlreadyClaimed(request.nino, request.saUtr, YEAR_2022, claimed2022.otherExpenses, claimed2022.jobExpenses, claimed2022.wasJobRateExpensesChecked)
          Some(Redirect(appConfig.p87DigitalFormUrl))
        case (_, _, _) => None
      }
    }
  }

  private def auditAlreadyClaimed(
                                   nino: String,
                                   saUtr: Option[String],
                                   year: Int,
                                   otherExpenses: Seq[IABDExpense],
                                   jobExpenses: Seq[IABDExpense],
                                   wasJobRateExpensesChecked: Boolean
                                 )(implicit hc: HeaderCarrier): Unit = {

    val json = if (wasJobRateExpensesChecked) {
      Json.obj( fields =
        "nino" -> nino,
        "saUtr" -> saUtr.getOrElse[String](""),
        s"taxYear" -> year,
        s"iabd-${appConfig.otherExpensesId}" -> otherExpenses,
        s"iabd-${appConfig.jobExpenseId}" -> jobExpenses
      )
    } else {
      Json.obj( fields =
        "nino" -> nino,
        "saUtr" -> saUtr.getOrElse[String](""),
        s"taxYear" -> year,
        s"iabd-${appConfig.otherExpensesId}" -> otherExpenses,
        s"iabd-${appConfig.jobExpenseId}" -> "NOT CHECKED"
      )
    }

    auditConnector.sendExplicitAudit(AlreadyClaimedExpenses.toString, json)
  }
}

trait CheckAlreadyClaimedAction extends ActionFilter[IdentifierRequest]
