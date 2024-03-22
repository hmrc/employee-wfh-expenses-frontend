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

import controllers.NumberOfWeeksToClaimForController.yearsWithoutWeeks
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ManualCorrespondenceIndicatorAction}
import forms.NumberOfWeeksToClaimForFormProvider
import models.TaxYearSelection
import models.TaxYearSelection._
import navigation.Navigator
import pages.{NumberOfWeeksToClaimForPage, SelectTaxYearsToClaimForPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NumberOfWeeksToClaimForView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
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
                                                 )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging with UIAssembler {

  def onPageLoad: Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(SelectTaxYearsToClaimForPage).map(_.filterNot(yearsWithoutWeeks.contains)) match {
        case Some(taxYear :: Nil) =>
          val preparedForm = request.userAnswers
            .get(NumberOfWeeksToClaimForPage)(NumberOfWeeksToClaimForPage.format)
            .flatMap(_.get(taxYear)) match {
            case None => formProvider(taxYear)
            case Some(value) => formProvider(taxYear).fill(value)
          }

          Ok(numberOfWeeksToClaimForView(preparedForm, taxYear))
        case Some(list) if list.size > 1 =>
          NotImplemented
        case _ =>
          Redirect(routes.IndexController.start)
      }
  }

  def onSubmit: Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(SelectTaxYearsToClaimForPage).map(_.filterNot(yearsWithoutWeeks.contains)) match {
        case Some(taxYear :: Nil) =>
          formProvider(taxYear).bindFromRequest().fold(
            formWithErrors => {
              Future.successful(BadRequest(numberOfWeeksToClaimForView(formWithErrors, taxYear)))
            },
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers
                  .set(NumberOfWeeksToClaimForPage, Map(taxYear -> value))(NumberOfWeeksToClaimForPage.format))
                _ <- sessionService.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(NumberOfWeeksToClaimForPage, updatedAnswers))
          )
        case Some(list) if list.size > 1 =>
          Future.successful(NotImplemented)
        case _ =>
          Future.successful(Redirect(routes.IndexController.start))
      }
  }
}

object NumberOfWeeksToClaimForController {
  val yearsWithoutWeeks: Seq[TaxYearSelection] = Seq(CurrentYearMinus2, CurrentYearMinus3, CurrentYearMinus4)
}