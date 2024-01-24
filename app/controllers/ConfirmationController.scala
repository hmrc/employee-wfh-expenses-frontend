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
import connectors.PaperlessPreferenceConnector
import controllers.PaperlessAuditConst._
import controllers.actions._
import javax.inject.Inject
import models.auditing.AuditEventType._
import models.requests.DataRequest
import pages.SubmittedClaim
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ConfirmationView

import scala.concurrent.{ExecutionContext, Future}

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
                                        confirmationView: ConfirmationView)
                                      (implicit ec: ExecutionContext) extends FrontendBaseController
  with I18nSupport with Logging with UIAssembler {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(SubmittedClaim) match {
        case Some(_) =>
          paperlessPreferenceConnector.getPaperlessStatus(s"${appConfig.pertaxFrontendHost}/personal-account") map {
            case Right(status) =>
              auditPaperlessPreferencesCheckSuccess(paperlessEnabled = status.isPaperlessCustomer)

              val selectedTaxYears = taxYearFromUIAssemblerFromRequest()

              Ok(confirmationView(
                status.isPaperlessCustomer, Some(status.url.link),
                selectedTaxYears.contains2021OrPrevious,
                selectedTaxYears.containsCurrent
              ))

            case Left(error) =>
              auditPaperlessPreferencesCheckFailure(error)
              Redirect(routes.TechnicalDifficultiesController.onPageLoad)
          }
        case None => Future.successful(Redirect(routes.IndexController.onPageLoad()))
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