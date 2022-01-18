/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.actions._
import forms.WhenDidYouFirstStartWorkingFromHomeFormProvider
import models.UserAnswers
import navigation.{Navigator, TaxYearFromUIAssembler}
import pages.{SelectTaxYearsToClaimForPage, WhenDidYouFirstStartWorkingFromHomePage}
import play.api.Logging
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.StartDateValidator
import views.html.WhenDidYouFirstStartWorkingFromHomeView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhenDidYouFirstStartWorkingFromHomeController @Inject()(
                                                               override val messagesApi: MessagesApi,
                                                               sessionRepository: SessionRepository,
                                                               navigator: Navigator,
                                                               identify: IdentifierAction,
                                                               citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                                               getData: DataRetrievalAction,
                                                               requireData: DataRequiredAction,
                                                               frontendAppConfig: FrontendAppConfig,
                                                               formProvider: WhenDidYouFirstStartWorkingFromHomeFormProvider,
                                                               val controllerComponents: MessagesControllerComponents,
                                                               whenDidYouFirstStartWorkingFromHomeView: WhenDidYouFirstStartWorkingFromHomeView
                                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with UIAssembler with Logging {

  val form = formProvider()

  val previousYearsSelectedMessageKey = frontendAppConfig.previousYearsSelectedMessageKey

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
      Ok(whenDidYouFirstStartWorkingFromHomeView(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData).async {
    implicit request =>

      val selectedTaxYears = taxYearFromUIAssemblerFromRequest()

      val validGivenOtherSelectedDates = StartDateValidator(selectedTaxYears.checkboxYearOptions, form.value).isStartDateValid

      val formWithPossibleValidationErrors = validGivenOtherSelectedDates match {
        case Some(validationResult) if validationResult._1 =>
          val formErrors = FormError("value", previousYearsSelectedMessageKey, Seq(validationResult._2.get))
          form.copy(errors = Seq(formErrors))
        case None => form
      }

      formWithPossibleValidationErrors.bindFromRequest().fold(
        formWithErrors => {
          val errors = formWithErrors.errors.map(error => error.copy(args = error.args.map(arg => "")))

          val filteredErrorList = if (errors.size > 1) {
            errors.filterNot(x => x.message == previousYearsSelectedMessageKey)
          } else {
            errors
          }
          Future.successful(BadRequest(whenDidYouFirstStartWorkingFromHomeView(formWithErrors.copy(errors = filteredErrorList))))
        },
        value => {
          val validSelectedDates = StartDateValidator(selectedTaxYears.checkboxYearOptions, Some(value)).isStartDateValid

          validSelectedDates match {
            case Some(validationResult) if !validationResult._1 =>
              val formErrors = FormError("value", previousYearsSelectedMessageKey, Seq(validationResult._2.get))
              Future.successful(BadRequest(whenDidYouFirstStartWorkingFromHomeView(form.fill(value).copy(errors = Seq(formErrors)))))
            case _ =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(WhenDidYouFirstStartWorkingFromHomePage, value))
                _ <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(WhenDidYouFirstStartWorkingFromHomePage, updatedAnswers))
          }
        }
      )
  }

}
