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
import models.{ClaimViewSettings, DisclaimerViewSettings}
import navigation.Navigator
import pages._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateLanguageTokenizer
import utils.TaxYearDates.TAX_YEAR_2021_START_DATE
import views.html.DisclaimerView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.Future

class DisclaimerController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      identify: IdentifierAction,
                                      citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                      sessionRepository: SessionRepository,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      navigator: Navigator,
                                      val controllerComponents: MessagesControllerComponents,
                                      disclaimerView: DisclaimerView
                                    ) extends FrontendBaseController with I18nSupport with UIAssembler {

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(SelectTaxYearsToClaimForPage) match {
        case Some(_) =>
          val selectedTaxYearsAssembler = taxYearFromUIAssemblerFromRequest()

          val startDate: Option[LocalDate] = if(selectedTaxYearsAssembler.containsPrevious) {
            request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage)
          } else {
            Some(TAX_YEAR_2021_START_DATE)
          }

          def buildDisclaimerPageSettings(dateList: List[(LocalDate, LocalDate)]) = {
            if (request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage).isDefined) {
              DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(dateList), Some(DateLanguageTokenizer.convertList(dateList)))))
            } else {
              DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(dateList), None)))
            }
          }

          Ok(disclaimerView(showBackLink = true, buildDisclaimerPageSettings(selectedTaxYearsAssembler.assemble), startDate))

        case None => Redirect(routes.IndexController.onPageLoad)
      }

  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(DisclaimerPage, request.userAnswers))
  }
}