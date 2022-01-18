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

import controllers.actions._
import models.ClaimViewSettings
import navigation.Navigator
import pages.WhenDidYouFirstStartWorkingFromHomePage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateLanguageTokenizer
import views.html._
import utils.TaxYearDates._

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CheckYourClaimController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         identify: IdentifierAction,
                                         checkAlreadyClaimed: CheckAlreadyClaimedAction,
                                         citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         submissionService: SubmissionService,
                                         navigator: Navigator,
                                         val controllerComponents: MessagesControllerComponents,
                                         checkYourClaimView: CheckYourClaimView,
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with UIAssembler {

  def onPageLoad: Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      val selectedTaxYears = taxYearFromUIAssemblerFromRequest()
      val startDate: Option[LocalDate] = request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage)
      val numberOfWeeksIn2019 = if(startDate.isDefined) numberOfWeeks(startDate.get, TAX_YEAR_2019_END_DATE) else 0

      def claimViewSettings(dateList: List[(LocalDate, LocalDate)]) = {
        ClaimViewSettings(DateLanguageTokenizer.convertList(dateList), Some(DateLanguageTokenizer.convertList(dateList)))
      }
      Ok(checkYourClaimView(claimViewSettings(selectedTaxYears.assemble), startDate, numberOfWeeksIn2019))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen checkAlreadyClaimed andThen getData andThen requireData).async {
    implicit request =>
      val selectedTaxYears = taxYearFromUIAssemblerFromRequest().checkboxYearOptions
      submissionService.submitExpenses(
        startDate = request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage),
        selectedTaxYears = selectedTaxYears
      ) map {
        case Right(_) =>
          Redirect(routes.ConfirmationController.onPageLoad())
        case Left(_) =>
          logger.error("[SubmitYourClaimController][onSubmit] - Error submitting")
          Redirect(routes.TechnicalDifficultiesController.onPageLoad())

      }
  }
}

