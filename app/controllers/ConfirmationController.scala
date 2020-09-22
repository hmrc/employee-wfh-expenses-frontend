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
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.ConfirmationView

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       val paperlessPreferenceConnector: PaperlessPreferenceConnector,
                                       appConfig: FrontendAppConfig,
                                       view: ConfirmationView)
                                      (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData).async {
    implicit request =>

      val preferencesSelfServiceUrl: String = s"${appConfig.pertaxFrontendHost}/personal-account"

      paperlessPreferenceConnector.getPaperlessPreference().flatMap {
        response =>
          response.getOrElse(false) match {
            case true => Future.successful(Ok(view(true, None)))
            case false => Future.successful(Ok(view(false, Some(preferencesSelfServiceUrl))))
          }
      }.recoverWith {
        case e =>
          Logger.error(s"[ConfirmationController][onPageLoad] failed: $e")
          Future.successful(Redirect(routes.TechnicalDifficultiesController.onPageLoad()))
      }
  }
}