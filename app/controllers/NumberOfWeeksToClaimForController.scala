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
import navigation.Navigator
import pages.NumberOfWeeksToClaimForPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NumberOfWeeksToClaimForView
import forms.NumberOfWeeksToClaimForFormProvider
import play.api.data.Form
import services.SessionService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NumberOfWeeksToClaimForController @Inject()(override val messagesApi: MessagesApi,
                                                  sessionService: SessionService,
                                                  identify: IdentifierAction,
                                                  citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                                  getData: DataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  navigator: Navigator,
                                                  numberOfWeeksToClaimForView: NumberOfWeeksToClaimForView,
                                                  formProvider: NumberOfWeeksToClaimForFormProvider,
                                                  val controllerComponents: MessagesControllerComponents
                                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with UIAssembler {

  val form: Form[Int] = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(NumberOfWeeksToClaimForPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(numberOfWeeksToClaimForView(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(numberOfWeeksToClaimForView(formWithErrors)))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(NumberOfWeeksToClaimForPage, value))
            _              <- sessionService.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(NumberOfWeeksToClaimForPage, updatedAnswers))
      )
  }
}
