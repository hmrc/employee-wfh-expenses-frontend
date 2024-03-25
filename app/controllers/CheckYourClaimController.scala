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
import models.TaxYearSelection.CurrentYearMinus1
import pages.{NumberOfWeeksToClaimForPage, SelectTaxYearsToClaimForPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html._
import utils.TaxYearFormatter

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

          val numberOfWeeksIn2023 = if (selectedTaxYears.contains2023) {
            request.userAnswers.get(NumberOfWeeksToClaimForPage).flatMap(_.get(CurrentYearMinus1))
          } else {
            None
          }

          val wholeTaxYears = selectedTaxYears.assembleWholeYears
          val formattedWholeTaxYears = TaxYearFormatter(wholeTaxYears).formattedTaxYears

          Ok(checkYourClaimView(formattedWholeTaxYears, numberOfWeeksIn2023, selectedTaxYears.checkboxYearOptions))
        case None =>
          Redirect(routes.IndexController.start)
      }


  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData).async {
    implicit request =>
      submissionService.submitExpenses(
        selectedTaxYears = request.userAnswers.get(SelectTaxYearsToClaimForPage).getOrElse(Nil),
        weeksForTaxYears = request.userAnswers.get(NumberOfWeeksToClaimForPage).getOrElse(Map())
      ) map {
        case Right(_) =>
          Redirect(routes.ConfirmationController.onPageLoad())
        case Left(_) =>
          logger.error("[SubmitYourClaimController][onSubmit] - Error submitting")
          Redirect(routes.TechnicalDifficultiesController.onPageLoad)
      }
  }
}

