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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ManualCorrespondenceIndicatorAction}
import forms.ConfirmClaimInWeeksFormProvider
import navigation.Navigator
import pages.{ConfirmClaimInWeeksPage, NumberOfWeeksToClaimForPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ConfirmClaimInWeeksView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmClaimInWeeksController @Inject()(override val messagesApi: MessagesApi,
                                              sessionService: SessionService,
                                              identify: IdentifierAction,
                                              citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              navigator: Navigator,
                                              confirmClaimInWeeksView: ConfirmClaimInWeeksView,
                                              formProvider: ConfirmClaimInWeeksFormProvider,
                                              val controllerComponents: MessagesControllerComponents
                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with UIAssembler {

  def onPageLoad: Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(NumberOfWeeksToClaimForPage).fold(Redirect(routes.SessionExpiredController.onPageLoad)) {
        numberOfWeeksToConfirm =>
          val form: Form[Boolean] = formProvider(numberOfWeeksToConfirm)

          val preparedForm = request.userAnswers.get(ConfirmClaimInWeeksPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(confirmClaimInWeeksView(preparedForm, numberOfWeeksToConfirm))
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(NumberOfWeeksToClaimForPage).fold(Future.successful(Redirect(routes.SessionExpiredController.onPageLoad))) {
        numberOfWeeksToConfirm =>
          val form: Form[Boolean] = formProvider(numberOfWeeksToConfirm)

          form.bindFromRequest().fold(
            formWithErrors => {
              Future.successful(BadRequest(confirmClaimInWeeksView(formWithErrors, numberOfWeeksToConfirm)))
            },
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ConfirmClaimInWeeksPage, value))
                _ <- sessionService.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(ConfirmClaimInWeeksPage, updatedAnswers))
          )
      }
  }
}
