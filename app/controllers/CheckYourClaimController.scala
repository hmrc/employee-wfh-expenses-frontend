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
import models.TaxYearSelection.wholeYearClaims
import pages.{NumberOfWeeksToClaimForPage, SelectTaxYearsToClaimForPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html._

import javax.inject.Inject
import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}

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
      (request.userAnswers.get(SelectTaxYearsToClaimForPage), request.userAnswers.get(NumberOfWeeksToClaimForPage)(NumberOfWeeksToClaimForPage.format)) match {
        case (Some(selectedTaxYears), optWeeksForTaxYears) if selectedTaxYears.nonEmpty
          && selectedTaxYears.diff(wholeYearClaims).forall(taxYear => optWeeksForTaxYears.exists(_.contains(taxYear))) =>

          Ok(checkYourClaimView(selectedTaxYears, optWeeksForTaxYears.getOrElse(ListMap())))
        case _ =>
          Redirect(routes.IndexController.start)
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData).async {
    implicit request =>
      (request.userAnswers.get(SelectTaxYearsToClaimForPage), request.userAnswers.get(NumberOfWeeksToClaimForPage)(NumberOfWeeksToClaimForPage.format)) match {
        case (Some(selectedTaxYears), optWeeksForTaxYears) if selectedTaxYears.nonEmpty
          && selectedTaxYears.diff(wholeYearClaims).forall(taxYear => optWeeksForTaxYears.exists(_.contains(taxYear))) =>

          submissionService.submitExpenses(
            selectedTaxYears = selectedTaxYears,
            weeksForTaxYears = optWeeksForTaxYears.getOrElse(ListMap())
          ) map {
            case Right(_) =>
              Redirect(routes.ConfirmationController.onPageLoad())
            case Left(_) =>
              logger.error("[SubmitYourClaimController][onSubmit] - Error submitting")
              Redirect(routes.TechnicalDifficultiesController.onPageLoad)
          }
        case _ =>
          Future.successful(Redirect(routes.IndexController.start))
      }
  }
}

