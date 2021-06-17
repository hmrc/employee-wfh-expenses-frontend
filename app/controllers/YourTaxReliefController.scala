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
import pages.WhenDidYouFirstStartWorkingFromHomePage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TaxYearDates._
import views.html._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class YourTaxReliefController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         identify: IdentifierAction,
                                         checkAlreadyClaimed: CheckAlreadyClaimedAction,
                                         citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         submissionService: SubmissionService,
                                         val controllerComponents: MessagesControllerComponents,
                                         yourTaxRelief2021OnlyView: YourTaxRelief2021OnlyView,
                                         yourTaxRelief2019_2020_2021View: YourTaxRelief2019_2020_2021View,
                                         yourTaxRelief2019_2020View: YourTaxRelief2019_2020View
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>

      (
        request.userAnswers.is2021Only,
        request.userAnswers.is2019And2020Only,
        request.userAnswers.is2019And2020And2021Only,
        request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage)
      ) match {
        case (true, _, _, _) => Ok(yourTaxRelief2021OnlyView())

        case (_, true, _, Some(date)) => Ok(yourTaxRelief2019_2020View(date, numberOfWeeks(date, TAX_YEAR_2019_END_DATE)))
        case (_, _, true, Some(date)) => Ok(yourTaxRelief2019_2020_2021View(date, numberOfWeeks(date, TAX_YEAR_2019_END_DATE)))

        case (_, true, _, None) => Redirect(routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad())
        case (_, _, true, None) => Redirect(routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad())

        case _ =>
          logger.error("[SubmitYourClaimController][onPageLoad] - No years to claim for found")
          Redirect(routes.TechnicalDifficultiesController.onPageLoad)
      }
  }


  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen checkAlreadyClaimed andThen getData andThen requireData).async {
    implicit request =>

      submissionService.submitExpenses(
        request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage),
        request.userAnswers.is2019And2020Only,
        request.userAnswers.is2019And2020And2021Only
      ) map {
        case Right(_) =>
          Redirect(routes.ConfirmationController.onPageLoad())
        case Left(_) =>
          logger.error("[SubmitYourClaimController][onSubmit] - Error submitting")
          Redirect(routes.TechnicalDifficultiesController.onPageLoad)

      }
  }

}
