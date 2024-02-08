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

import controllers.actions._
import forms.WhenDidYouFirstStartWorkingFromHomeFormProvider
import models.{Date, SelectTaxYearsToClaimFor}
import navigation.Navigator
import pages.{SelectTaxYearsToClaimForPage, WhenDidYouFirstStartWorkingFromHomePage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.WhenDidYouFirstStartWorkingFromHomeView

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

class WhenDidYouFirstStartWorkingFromHomeController @Inject()(override val messagesApi: MessagesApi,
                                                              sessionService: SessionService,
                                                              navigator: Navigator,
                                                              identify: IdentifierAction,
                                                              citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                                              getData: DataRetrievalAction,
                                                              requireData: DataRequiredAction,
                                                              formProvider: WhenDidYouFirstStartWorkingFromHomeFormProvider,
                                                              val controllerComponents: MessagesControllerComponents,
                                                              whenDidYouFirstStartWorkingFromHomeView: WhenDidYouFirstStartWorkingFromHomeView
                                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      val messages = request2Messages
      val form: Form[Date] = formProvider(messages)
      request.userAnswers.get(SelectTaxYearsToClaimForPage) match {
        case Some(selectedTaxYears) =>
          val isClaimingCTY = selectedTaxYears.contains(SelectTaxYearsToClaimFor.Option1)
          val preparedForm = request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) match {
            case None => form
            case Some(value) => form.fill(value)
          }
          Ok(whenDidYouFirstStartWorkingFromHomeView(preparedForm, isClaimingCTY))
        case None => Redirect(routes.IndexController.onPageLoad())
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData).async {
    implicit request =>
      val messages = request2Messages
      val selectedTaxYears = request.userAnswers.get(SelectTaxYearsToClaimForPage)
      val form: Form[Date] = formProvider(messages)
      form.bindFromRequest().fold(
        formWithErrors => {
          val errors = formWithErrors.errors.map(error => error.copy(args = error.args.map(arg => messages(s"date.$arg").toLowerCase)))
          val isClaimingCTY = selectedTaxYears.contains(SelectTaxYearsToClaimFor.Option1)
          Future.successful(BadRequest(whenDidYouFirstStartWorkingFromHomeView(formWithErrors.copy(errors = errors), isClaimingCTY)))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(WhenDidYouFirstStartWorkingFromHomePage, value))
            _ <- sessionService.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(WhenDidYouFirstStartWorkingFromHomePage, updatedAnswers))
      )
  }

}
