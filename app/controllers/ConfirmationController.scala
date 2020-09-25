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

package controllers

import config.FrontendAppConfig
import connectors.PaperlessPreferenceConnector
import controllers.PaperlessAuditConst._
import controllers.actions._
import javax.inject.Inject
import models.auditing.AuditEventType._
import models.requests.DataRequest
import pages.WhenDidYouFirstStartWorkingFromHomePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear
import uk.gov.hmrc.time.TaxYear.taxYearFor
import views.html.ConfirmationView

import scala.concurrent.ExecutionContext

private object PaperlessAuditConst {
  val NinoReference = "nino"
  val Enabled = "paperlessEnabled"
  val FailureReason = "reasonForFailure"
}

class ConfirmationController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        val paperlessPreferenceConnector: PaperlessPreferenceConnector,
                                        auditConnector: AuditConnector,
                                        appConfig: FrontendAppConfig,
                                        view: ConfirmationView)
                                      (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val taxYearStartedWorkingFromHome: Option[TaxYear] = request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage).map(taxYearFor)

      paperlessPreferenceConnector.getPaperlessStatus(s"${appConfig.pertaxFrontendHost}/personal-account") map {

        case Right(status) if status.isPaperlessCustomer =>
          auditPaperlessPreferencesCheckSuccess(paperlessEnabled = true)
          Ok(view(
            paperLessAvailable  = true,
            paperlessSignupUrl  = None,
            startedInTaxYear    = taxYearStartedWorkingFromHome))

        case Right(status) =>
          auditPaperlessPreferencesCheckSuccess(paperlessEnabled = false)
          Ok(view(
            paperLessAvailable  = false,
            paperlessSignupUrl  = Some(status.url.link),
            startedInTaxYear    = taxYearStartedWorkingFromHome))

        case Left(error) =>
          auditPaperlessPreferencesCheckFailure(error)
          Redirect(routes.TechnicalDifficultiesController.onPageLoad())
      }
  }


  private def auditPaperlessPreferencesCheckSuccess(paperlessEnabled:Boolean)
                                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Unit =
    auditConnector.sendExplicitAudit(
      PaperlessPreferenceCheckSuccess.toString,
      Map(
        NinoReference -> dataRequest.nino,
        Enabled       -> paperlessEnabled.toString
      )
    )

  private def auditPaperlessPreferencesCheckFailure(error: String)
                                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Unit =
    auditConnector.sendExplicitAudit(
      PaperlessPreferenceCheckFailure.toString,
      Map(
        NinoReference -> dataRequest.nino,
        FailureReason -> error
      )
    )

}