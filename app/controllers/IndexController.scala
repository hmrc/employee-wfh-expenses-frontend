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
import repositories.SessionRepository
import services.IABDService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TaxYearDates.{YEAR_2020, YEAR_2021, YEAR_2022}

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait TaiLookupHandler extends Logging {

  val iabdService: IABDService
  val navigator: Navigator
  val sessionRepository: SessionRepository
  val appConfig: FrontendAppConfig
  val auditConnector: AuditConnector

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

  def handlePageRequest(successHandler: (Boolean, Boolean, Boolean) => Result)
                       (implicit dataRequest: OptionalDataRequest[AnyContent], hc: HeaderCarrier): Future[Result] = {


    val alreadyClaimed2020Future = iabdService.alreadyClaimed(dataRequest.nino, YEAR_2020)
    val alreadyClaimed2021Future = iabdService.alreadyClaimed(dataRequest.nino, YEAR_2021)
    val alreadyClaimed2022Future = iabdService.alreadyClaimed(dataRequest.nino, YEAR_2022)

    for {
      alreadyClaimed2020 <- alreadyClaimed2020Future
      alreadyClaimed2021 <- alreadyClaimed2021Future
      alreadyClaimed2022 <- alreadyClaimed2022Future
    } yield {
      (alreadyClaimed2020, alreadyClaimed2021, alreadyClaimed2022) match {
        case (Some(claimed2020), Some(claimed2021), Some(claimed2022)) =>
          logger.info(s"[TaiLookupHandler][handlePageRequest] Detected already claimed for $YEAR_2020, $YEAR_2021 and $YEAR_2022; redirecting to P87 digital form")
          auditAlreadyClaimed(dataRequest.nino, dataRequest.saUtr, YEAR_2020, claimed2020.otherExpenses, claimed2020.jobExpenses, claimed2020.wasJobRateExpensesChecked)
          auditAlreadyClaimed(dataRequest.nino, dataRequest.saUtr, YEAR_2021, claimed2021.otherExpenses, claimed2021.jobExpenses, claimed2021.wasJobRateExpensesChecked)
          auditAlreadyClaimed(dataRequest.nino, dataRequest.saUtr, YEAR_2022, claimed2022.otherExpenses, claimed2022.jobExpenses, claimed2022.wasJobRateExpensesChecked)
          Redirect(appConfig.p87DigitalFormUrl)
        case (_, _, _) =>
          successHandler(alreadyClaimed2020.isDefined, alreadyClaimed2021.isDefined, alreadyClaimed2022.isDefined)
      }
    }
  }.recoverWith {
    case ex: Exception =>
      val message = s"TaiIndexLookupService lookup failed with: ${ex.getMessage}"
      logger.error(message)
      Future.failed(new RuntimeException(message))
  }

  def taiLookupSuccessHandler(alreadyClaimed2020: Boolean,
                              alreadyClaimed2021: Boolean,
                              alreadyClaimed2022: Boolean)
                             (implicit request: OptionalDataRequest[AnyContent]): Result = {

    val eligibilityCheckerSessionIdString = request.queryString.get("eligibilityCheckerSessionId") match {
      case Some(sessionIdSeq) => sessionIdSeq.head
      case None => ""
    }

    val answers = UserAnswers(
      request.internalId,
      Json.obj(
        ClaimedForTaxYear2020.toString -> alreadyClaimed2020,
        ClaimedForTaxYear2021.toString -> alreadyClaimed2021,
        ClaimedForTaxYear2022.toString -> alreadyClaimed2022,
        HasSelfAssessmentEnrolment.toString -> request.saUtr.isDefined,
        EligibilityCheckerSessionId.toString() -> eligibilityCheckerSessionIdString
      )
    )

    sessionRepository.set(answers)

    val navigatorPageResult: Call = navigator.nextPage(ClaimedForTaxYear2020, answers)

    Redirect(navigatorPageResult)
  }
}



class IndexController @Inject()(
                                 val controllerComponents: MessagesControllerComponents,
                                 sessionRepositoryInput: SessionRepository,
                                 identify: IdentifierAction,
                                 citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                 navigatorInput: Navigator,
                                 getData: DataRetrievalAction,
                                 iabdServiceInput: IABDService,
                                 appConfigInput: FrontendAppConfig,
                                 auditConnectorInput: AuditConnector) extends FrontendBaseController
  with TaiLookupHandler with I18nSupport {

  val iabdService: IABDService = iabdServiceInput
  val navigator: Navigator = navigatorInput
  val sessionRepository: SessionRepository = sessionRepositoryInput
  val appConfig: FrontendAppConfig = appConfigInput
  val auditConnector: AuditConnector = auditConnectorInput

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData).async {
    implicit request => {
      handlePageRequest(taiLookupSuccessHandler)
    }
  }

}