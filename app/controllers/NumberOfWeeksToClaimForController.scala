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

import controllers.actions.{
  DataRequiredAction,
  DataRetrievalAction,
  IdentifierAction,
  ManualCorrespondenceIndicatorAction
}
import forms.NumberOfWeeksToClaimForFormProvider
import models.TaxYearSelection._
import navigation.Navigator
import pages.{NumberOfWeeksToClaimForPage, SelectTaxYearsToClaimForPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{NumberOfWeeksToClaimForMultipleYearsView, NumberOfWeeksToClaimForView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NumberOfWeeksToClaimForController @Inject() (
    override val messagesApi: MessagesApi,
    sessionService: SessionService,
    identify: IdentifierAction,
    citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    navigator: Navigator,
    numberOfWeeksToClaimForView: NumberOfWeeksToClaimForView,
    numberOfWeeksToClaimForMultipleYearsView: NumberOfWeeksToClaimForMultipleYearsView,
    formProvider: NumberOfWeeksToClaimForFormProvider,
    val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    identify.andThen(citizenDetailsCheck).andThen(getData).andThen(requireData) { implicit request =>
      request.userAnswers.get(SelectTaxYearsToClaimForPage).map(_.filterNot(wholeYearClaims.contains)) match {
        case Some(list) if list.nonEmpty =>
          val preparedForm =
            request.userAnswers.get(NumberOfWeeksToClaimForPage)(NumberOfWeeksToClaimForPage.format) match {
              case None        => formProvider(list)
              case Some(value) => formProvider(list).fill(value)
            }

          if (list.size > 1) {
            Ok(numberOfWeeksToClaimForMultipleYearsView(preparedForm, list))
          } else {
            Ok(numberOfWeeksToClaimForView(preparedForm, list.head))
          }
        case _ =>
          Redirect(routes.IndexController.start)
      }
    }

  def onSubmit: Action[AnyContent] =
    identify.andThen(citizenDetailsCheck).andThen(getData).andThen(requireData).async { implicit request =>
      request.userAnswers.get(SelectTaxYearsToClaimForPage).map(_.filterNot(wholeYearClaims.contains)) match {
        case Some(list) if list.nonEmpty =>
          formProvider(list)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                if (list.size > 1) {
                  Future.successful(BadRequest(numberOfWeeksToClaimForMultipleYearsView(formWithErrors, list)))
                } else {
                  Future.successful(BadRequest(numberOfWeeksToClaimForView(formWithErrors, list.head)))
                },
              value =>
                for {
                  updatedAnswers <- Future.fromTry(
                    request.userAnswers
                      .set(NumberOfWeeksToClaimForPage, value)(NumberOfWeeksToClaimForPage.format)
                  )
                  _ <- sessionService.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(NumberOfWeeksToClaimForPage, updatedAnswers))
            )
        case _ =>
          Future.successful(Redirect(routes.IndexController.start))
      }
    }

}
