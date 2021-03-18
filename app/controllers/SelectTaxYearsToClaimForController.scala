/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.SelectTaxYearsToClaimForFormProvider

import javax.inject.Inject
import navigation.Navigator
import pages.{ClaimedForTaxYear2020, SelectTaxYearsToClaimForPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SelectTaxYearsToClaimForView

import scala.concurrent.{ExecutionContext, Future}

class SelectTaxYearsToClaimForController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: SelectTaxYearsToClaimForFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: SelectTaxYearsToClaimForView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      request.userAnswers.get(ClaimedForTaxYear2020) match {
        case Some(claimedAlready) if claimedAlready  =>
          Redirect(routes.DisclaimerController.onPageLoad())

        case Some(claimedAlready) if !claimedAlready =>
          val preparedForm = request.userAnswers.get(SelectTaxYearsToClaimForPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }
          Ok(view(preparedForm))

        case None => Redirect(routes.IndexController.onPageLoad())
      }

  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectTaxYearsToClaimForPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(SelectTaxYearsToClaimForPage, updatedAnswers))
      )
  }
}
