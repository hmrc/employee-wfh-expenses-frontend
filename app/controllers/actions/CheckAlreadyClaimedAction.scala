/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.TaiConnector
import controllers.routes
import models.IABDExpense
import models.auditing.AuditEventType.AlreadyClaimedExpenses
import models.requests.IdentifierRequest
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.TaxYearDates.TAX_YEAR_2020_START_DATE

import scala.concurrent.{ExecutionContext, Future}


class CheckAlreadyClaimedActionImpl @Inject()(
                                               appConfig: FrontendAppConfig,
                                               taiConnector: TaiConnector,
                                               auditConnector: AuditConnector,
                                               val parser: BodyParsers.Default
                                             )(implicit val executionContext: ExecutionContext) extends CheckAlreadyClaimedAction {

  override protected def filter[A](request: IdentifierRequest[A]): Future[Option[Result]] = {

    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    { for {
      otherExpenses   <- taiConnector.getOtherExpensesData(request.nino, TAX_YEAR_2020_START_DATE.getYear)
      otherRateAmount = otherExpenses.map(_.grossAmount).sum
      jobExpenses     <-
          if (otherRateAmount==0) {
            taiConnector.getJobExpensesData(request.nino, TAX_YEAR_2020_START_DATE.getYear)
          } else {
            Future.successful(Seq[IABDExpense]())
          }
      jobRateAmount             = jobExpenses.map(_.grossAmount).sum
      wasJobRateExpensesChecked = if (otherRateAmount == 0) true else false
      } yield
        if (otherRateAmount > 0 || jobRateAmount > 0) {
          Logger.info("[CheckAlreadyClaimedAction][filter] Detected already claimed, redirecting to P87 digital form")
          auditAlreadyClaimed[A](otherExpenses, jobExpenses, wasJobRateExpensesChecked)(request, hc)
          Some(Redirect(appConfig.p87DigitalFormUrl))
        } else {
          None
        }
    } recover {
      case e: Exception =>
        Logger.error(s"[CheckAlreadyClaimedAction][filter] failed: $e")
        Some(Redirect(routes.TechnicalDifficultiesController.onPageLoad()))
    }


  }

  private def auditAlreadyClaimed[A](otherExpenses: Seq[IABDExpense], jobExpenses: Seq[IABDExpense], wasJobRateExpensesChecked: Boolean)
                                 (implicit request: IdentifierRequest[A], hc: HeaderCarrier) = {

    val json = if (wasJobRateExpensesChecked) {
        Json.obj(
          "nino" -> request.nino,
          s"iabd-${appConfig.otherExpensesId}"  -> otherExpenses,
          s"iabd-${appConfig.jobExpenseId}"     -> jobExpenses
        )
    } else {
        Json.obj(
          "nino" -> request.nino,
          s"iabd-${appConfig.otherExpensesId}"  -> otherExpenses,
          s"iabd-${appConfig.jobExpenseId}"     -> "NOT CHECKED"
        )
    }

    auditConnector.sendExplicitAudit(AlreadyClaimedExpenses.toString, json)
  }
}

trait CheckAlreadyClaimedAction extends ActionFilter[IdentifierRequest]
