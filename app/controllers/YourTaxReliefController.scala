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

import javax.inject.Inject
import pages.WhenDidYouFirstStartWorkingFromHomePage
import play.api.{Logger, Logging}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear
import utils.TaxYearDates._
import views.html.{YourTaxRelief2019And2020View, YourTaxRelief2020OnlyView}

import scala.concurrent.{ExecutionContext, Future}

class YourTaxReliefController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       checkAlreadyClaimed: CheckAlreadyClaimedAction,
                                       citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       submissionService: SubmissionService,
                                       sessionRepository: SessionRepository,
                                       val controllerComponents: MessagesControllerComponents,
                                       yourTaxRelief2019And2020View: YourTaxRelief2019And2020View,
                                       yourTaxRelief2020OnlyView: YourTaxRelief2020OnlyView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) match {

        case None =>
          Redirect(routes.DisclaimerController.onPageLoad())

        case Some(date) if TaxYear(2019).contains(date) =>
          Ok(
            yourTaxRelief2019And2020View(date, numberOfWeeks(date, TAX_YEAR_2019_END_DATE))
          )

        case Some(date) if TaxYear(2020).contains(date) =>
          Ok(
            yourTaxRelief2020OnlyView(date)
          )

        case Some(date) =>
          logger.error(s"[YourTaxReliefController][onPageLoad] Received an unexpected date : $date")
          Redirect(routes.TechnicalDifficultiesController.onPageLoad())
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen checkAlreadyClaimed andThen getData andThen requireData).async {
    implicit request =>

      request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) match {

        case None =>
          logger.error(s"[SubmitYourClaimController][onSubmit] - Start date is missing")
          Future.successful( Redirect(routes.TechnicalDifficultiesController.onPageLoad()) )

        case Some(startDate) =>
          submissionService.submitExpenses(startDate) map {

            case Right(_) =>
              Redirect(routes.ConfirmationController.onPageLoad())

            case Left(_) =>
              Redirect(routes.TechnicalDifficultiesController.onPageLoad())
          }
      }
  }

}
