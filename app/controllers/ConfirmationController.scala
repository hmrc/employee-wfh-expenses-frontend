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

import connectors.CitizenDetailsConnector
import controllers.actions._
import javax.inject.Inject
import models.Address
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.ConfirmationView

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       checkAlreadyClaimed: CheckAlreadyClaimedAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       citizenDetailsConnector: CitizenDetailsConnector,
                                       view: ConfirmationView)
                                      (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen checkAlreadyClaimed andThen getData andThen requireData).async {
    implicit request =>

      citizenDetailsConnector.getAddress(request.nino).flatMap {
        response =>
          response.status match {
            case OK =>
              Json.parse(response.body).validate[Address] match {
                case JsSuccess(address, _) => Future.successful(Ok(view()))
              }
            case LOCKED =>
              Future.successful(Redirect(routes.ManualCorrespondenceIndicatorController.onPageLoad()))
            case _ =>
              Future.successful(Redirect(routes.TechnicalDifficultiesController.onPageLoad()))
          }
      }.recoverWith {
        case e =>
          Logger.error(s"[ConfirmationController][onPageLoad] failed: $e")
          Future.successful(Redirect(routes.TechnicalDifficultiesController.onPageLoad()))
      }

  }
}
