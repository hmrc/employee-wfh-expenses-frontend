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
import controllers.actions._
import javax.inject.Inject
import models.requests.DataRequest
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.ConfirmationView

import scala.concurrent.{ExecutionContext, Future}

private object PaperlessAuditConst {
  val AuditReference = "PaperlessPreferenceAudit"
  val NinoReference = "nino"
  val Enabled = "paperlessEnabled"
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

      val preferencesSelfServiceUrl = s"${appConfig.pertaxFrontendHost}/personal-account"

      paperlessPreferenceConnector.getPaperlessPreference().flatMap {
        response =>
          response.getOrElse(false) match {
            case true => {
              auditPaperlessPreferences(true)
              Future.successful(Ok(view(true, None)))
            }
            case false => {
              auditPaperlessPreferences(false)
              Future.successful(Ok(view(false, Some(preferencesSelfServiceUrl))))
            }
          }
      }.recoverWith {
        case e =>
          Logger.error(s"[ConfirmationController][onPageLoad] failed: $e")
          Future.successful(Redirect(routes.TechnicalDifficultiesController.onPageLoad()))
      }
  }

  private def auditPaperlessPreferences(paperlessEnabled: Boolean)
                                       (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext) = {
    import PaperlessAuditConst._

    val dataToAudit = Map(NinoReference -> dataRequest.nino, Enabled -> paperlessEnabled.toString)

    auditConnector.sendExplicitAudit(AuditReference, dataToAudit)
  }
}