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
import pages.CitizensDetailsAddress
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
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ConfirmationView,
                                       citizenDetailsConnector: CitizenDetailsConnector
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

//      citizenDetailsConnector.getAddress("GC123456A").flatMap {
//        response =>
//          response.status match {
//            case OK =>
//              Json.parse(response.body).validate[Address] match {
//                case JsSuccess(address, _) if address.line1.exists(_.trim.nonEmpty) && address.postcode.exists(_.trim.nonEmpty) =>
//                  for {
//                    updatedAnswers <- Future.fromTry(request.userAnswers.set(CitizensDetailsAddress, address))
//                    _ <- sessionRepository.set(updatedAnswers)
//                  } yield {
//                    Redirect(navigator.nextPage(YourAddressPage, mode, updatedAnswers))
//                  }
//                case _ =>
//                  Future.successful(Redirect(navigator.nextPage(YourAddressPage, mode, request.userAnswers)))
//              }
//            case LOCKED =>
//              Future.successful(Redirect(ContactUsController.onPageLoad()))
//            case _ =>
//              Future.successful(Redirect(navigator.nextPage(YourAddressPage, mode, request.userAnswers)))
//          }
//      }.recoverWith {
//        case e =>
//          Logger.warn(s"[YourAddressController][citizenDetailsConnector.getAddress] failed: $e")
//          Future.successful(Redirect(TechnicalDifficultiesController.onPageLoad()))
//      }

      val citizenAddress = Address(Some("line 1"), Some("line 2"), Some("line 3"), Some("line 4"),Some("line 5"),Some("post code"), Some("country"))

      Ok(view(citizenAddress))
  }
}
