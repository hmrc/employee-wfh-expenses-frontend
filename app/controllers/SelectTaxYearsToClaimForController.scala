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
import forms.SelectTaxYearsToClaimForFormProvider
import models.TaxYearSelection
import navigation.Navigator
import pages._
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SelectTaxYearsToClaimForView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectTaxYearsToClaimForController @Inject()(override val messagesApi: MessagesApi,
                                                   sessionService: SessionService,
                                                   navigator: Navigator,
                                                   identify: IdentifierAction,
                                                   getData: DataRetrievalAction,
                                                   requireData: DataRequiredAction,
                                                   formProvider: SelectTaxYearsToClaimForFormProvider,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: SelectTaxYearsToClaimForView
                                                  )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[Seq[TaxYearSelection]] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      (
        request.userAnswers.get(ClaimedForTaxYear2020),
        request.userAnswers.get(ClaimedForTaxYear2021),
        request.userAnswers.get(ClaimedForTaxYear2022),
        request.userAnswers.get(ClaimedForTaxYear2023),
        request.userAnswers.get(ClaimedForTaxYear2024)
      ) match {
        case (Some(claimed2020), Some(claimed2021), Some(claimed2022), Some(claimed2023), Some(claimed2024)) =>
          val availableYears = TaxYearSelection.getValuesFromClaimedBooleans(claimed2020, claimed2021, claimed2022, claimed2023, claimed2024)

          val preparedForm = request.userAnswers.get(SelectTaxYearsToClaimForPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Future.successful(Ok(view(preparedForm, availableYears)))
        case _ =>
          Future.successful(Redirect(routes.IndexController.start))
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val availableYearsUserCanClaim = TaxYearSelection.getValuesFromClaimedBooleans(
        request.userAnswers.get(ClaimedForTaxYear2020).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2021).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2022).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2023).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2024).getOrElse(false)
      )

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, availableYearsUserCanClaim))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectTaxYearsToClaimForPage, value))
            _ <- sessionService.set(updatedAnswers)
          } yield {
            Redirect(navigator.nextPage(SelectTaxYearsToClaimForPage, updatedAnswers))
          }
      )
  }

}
