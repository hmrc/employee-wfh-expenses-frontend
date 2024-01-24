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

package controllers

import config.FrontendAppConfig
import controllers.actions.{DataRetrievalAction, IdentifierAction, ManualCorrespondenceIndicatorAction}
import models.{IABDExpense, UserAnswers}
import models.auditing.AuditEventType.AlreadyClaimedExpenses
import models.requests.OptionalDataRequest
import navigation.Navigator
import pages._
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.{IABDService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TaxYearDates.{YEAR_2020, YEAR_2021, YEAR_2022, YEAR_2023}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait TaiLookupHandler extends Logging {

  val iabdService: IABDService
  val navigator: Navigator
  val sessionService: SessionService
  val appConfig: FrontendAppConfig
  val auditConnector: AuditConnector

  private def auditAlreadyClaimed(
                                   nino: String,
                                   saUtr: Option[String],
                                   year: Int,
                                   otherExpenses: Seq[IABDExpense],
                                   jobExpenses: Seq[IABDExpense],
                                   wasJobRateExpensesChecked: Boolean
                                 )(implicit hc: HeaderCarrier,
                                   executionContext: ExecutionContext): Unit = {

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

  def handlePageRequest(successHandler: (Boolean, Boolean, Boolean, Boolean) => Result)
                       (implicit dataRequest: OptionalDataRequest[AnyContent], hc: HeaderCarrier, executionContext: ExecutionContext): Future[Result] = {


    val alreadyClaimed2020Future = iabdService.alreadyClaimed(dataRequest.nino, YEAR_2020)
    val alreadyClaimed2021Future = iabdService.alreadyClaimed(dataRequest.nino, YEAR_2021)
    val alreadyClaimed2022Future = iabdService.alreadyClaimed(dataRequest.nino, YEAR_2022)
    val alreadyClaimed2023Future = iabdService.alreadyClaimed(dataRequest.nino, YEAR_2023)

    for {
      alreadyClaimed2020 <- alreadyClaimed2020Future
      alreadyClaimed2021 <- alreadyClaimed2021Future
      alreadyClaimed2022 <- alreadyClaimed2022Future
      alreadyClaimed2023 <- alreadyClaimed2023Future
    } yield {
      (alreadyClaimed2020, alreadyClaimed2021, alreadyClaimed2022, alreadyClaimed2023) match {
        case (Some(claimed2020), Some(claimed2021), Some(claimed2022), Some(claimed2023)) =>
          logger.info(s"[TaiLookupHandler][handlePageRequest] Detected already claimed for $YEAR_2020, $YEAR_2021, $YEAR_2022 and $YEAR_2023; redirecting to P87 digital form")
          auditAlreadyClaimed(dataRequest.nino, dataRequest.saUtr, YEAR_2020, claimed2020.otherExpenses, claimed2020.jobExpenses, claimed2020.wasJobRateExpensesChecked)
          auditAlreadyClaimed(dataRequest.nino, dataRequest.saUtr, YEAR_2021, claimed2021.otherExpenses, claimed2021.jobExpenses, claimed2021.wasJobRateExpensesChecked)
          auditAlreadyClaimed(dataRequest.nino, dataRequest.saUtr, YEAR_2022, claimed2022.otherExpenses, claimed2022.jobExpenses, claimed2022.wasJobRateExpensesChecked)
          auditAlreadyClaimed(dataRequest.nino, dataRequest.saUtr, YEAR_2023, claimed2023.otherExpenses, claimed2023.jobExpenses, claimed2023.wasJobRateExpensesChecked)
          Redirect(appConfig.p87DigitalFormUrl)
        case (_, _, _, _) =>
          successHandler(alreadyClaimed2020.isDefined, alreadyClaimed2021.isDefined, alreadyClaimed2022.isDefined, alreadyClaimed2023.isDefined)
      }
    }
  }.recoverWith {
    case ex: Exception =>
      val message = s"TaiIndexLookupService lookup failed with: ${ex.getMessage}"
      logger.error(message)
      Future.failed(ex)
  }

  def taiLookupSuccessHandler(isMergedJourney: Boolean)
                             (alreadyClaimed2020: Boolean,
                              alreadyClaimed2021: Boolean,
                              alreadyClaimed2022: Boolean,
                              alreadyClaimed2023: Boolean)
                             (implicit request: OptionalDataRequest[AnyContent], hc: HeaderCarrier): Result = {

    val answers = UserAnswers(
      request.internalId,
      Json.obj(
        MergedJourneyFlag.toString -> (isMergedJourney && appConfig.mergedJourneyEnabled),
        ClaimedForTaxYear2020.toString -> alreadyClaimed2020,
        ClaimedForTaxYear2021.toString -> alreadyClaimed2021,
        ClaimedForTaxYear2022.toString -> alreadyClaimed2022,
        ClaimedForTaxYear2023.toString -> alreadyClaimed2023,
        HasSelfAssessmentEnrolment.toString -> (if(appConfig.saLookupEnabled) request.saUtr.isDefined else false)
      )
    )

    sessionService.set(answers)

    val navigatorPageResult: Call = navigator.nextPage(ClaimedForTaxYear2020, answers)

    Redirect(navigatorPageResult)
  }
}


class IndexController @Inject()(val controllerComponents: MessagesControllerComponents,
                                val sessionService: SessionService,
                                identify: IdentifierAction,
                                citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                val navigator: Navigator,
                                getData: DataRetrievalAction,
                                val iabdService: IABDService,
                                val appConfig: FrontendAppConfig,
                                val auditConnector: AuditConnector
                               )(implicit executionContext: ExecutionContext)
  extends FrontendBaseController with TaiLookupHandler with I18nSupport {

  def onPageLoad(isMergedJourney: Boolean = false): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData).async {
    implicit request => {
      handlePageRequest(taiLookupSuccessHandler(isMergedJourney))
    }
  }

}