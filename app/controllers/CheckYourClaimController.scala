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
import models.Date
import pages.{NumberOfWeeksToClaimForPage, SelectTaxYearsToClaimForPage, WhenDidYouFirstStartWorkingFromHomePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TaxYearDates._
import views.html._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CheckYourClaimController @Inject()(override val messagesApi: MessagesApi,
                                         identify: IdentifierAction,
                                         citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         submissionService: SubmissionService,
                                         val controllerComponents: MessagesControllerComponents,
                                         checkYourClaimView: CheckYourClaimView,
                                        )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging with UIAssembler {

  def onPageLoad: Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(SelectTaxYearsToClaimForPage) match {
        case Some(_) =>
          val selectedTaxYears = taxYearFromUIAssemblerFromRequest()

          val startDate: Option[Date] = request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage)

          val numberOfWeeksIn2019 = if (startDate.isDefined) {
            numberOfWeeks(startDate.get.date, TAX_YEAR_2019_END_DATE)
          } else {
            0
          }

          val numberOfWeeksIn2023 = if (selectedTaxYears.contains2023) {
            request.userAnswers.get(NumberOfWeeksToClaimForPage)
          } else {
            None
          }

          Ok(checkYourClaimView(claimViewSettings(selectedTaxYears.assembleWholeYears), startDate, numberOfWeeksIn2019,
            numberOfWeeksIn2023, selectedTaxYears.checkboxYearOptions))
        case None =>
          Redirect(routes.IndexController.start)
      }


  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData).async {
    implicit request =>
      val selectedTaxYears = taxYearFromUIAssemblerFromRequest()
      val startDate = if (selectedTaxYears.contains2020) request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) else None
      val numberOfWeeksOf2023 = if (selectedTaxYears.contains2023) request.userAnswers.get(NumberOfWeeksToClaimForPage) else None

      submissionService.submitExpenses(
        startDate = startDate,
        selectedTaxYears = selectedTaxYears.checkboxYearOptions,
        numberOfWeeksOf2023 = numberOfWeeksOf2023
      ) map {
        case Right(_) =>
          Redirect(routes.ConfirmationController.onPageLoad())
        case Left(_) =>
          logger.error("[SubmitYourClaimController][onSubmit] - Error submitting")
          Redirect(routes.TechnicalDifficultiesController.onPageLoad)
      }
  }
}

