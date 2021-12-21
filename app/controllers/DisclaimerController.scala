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
import models.{ClaimViewSettings, DisclaimerViewSettings}
import navigation.{Navigator, SelectedTaxYears}
import pages.{DisclaimerPage, SelectTaxYearsToClaimForPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateLanguageTokenizer
import views.html.DisclaimerView

import java.time.LocalDate
import javax.inject.Inject

class DisclaimerController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      identify: IdentifierAction,
                                      citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      navigator: Navigator,
                                      val controllerComponents: MessagesControllerComponents,
                                      disclaimerView: DisclaimerView
                                    ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>

      val selectedOptionsCheckBoxes = request.userAnswers.get(SelectTaxYearsToClaimForPage).getOrElse(Nil).map(_.toString).toList

      val selectedTaxYears = SelectedTaxYears(selectedOptionsCheckBoxes)

      def disclaimerSettings(dateList: List[(LocalDate, LocalDate)]) = {
        DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(dateList), Some(DateLanguageTokenizer.convertList(dateList)))))
      }

      Ok(disclaimerView(showBackLink = false, disclaimerSettings(selectedTaxYears.select())))

  }

  /*
  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>

      request.userAnswers.get(HasSelfAssessmentEnrolment) match {
        case None        => Redirect(routes.IndexController.onPageLoad())
        case Some(true)  => Ok(disclaimer2021View(false))
        case Some(false) =>
          (request.userAnswers.get(ClaimedForTaxYear2020), request.userAnswers.get(SelectTaxYearsToClaimForPage) ) match {
            case (Some(true), _)      => Ok(disclaimer2021View(true))

            case (Some(false), None)  => Redirect(routes.SelectTaxYearsToClaimForController.onPageLoad())

            case (Some(false), Some(yearsToClaimFor)) => yearsToClaimFor.size match {
              case 0 => Redirect(routes.SelectTaxYearsToClaimForController.onPageLoad())
              case 2 => Ok(disclaimer2019_2020_2021View())
              case 1 => yearsToClaimFor.head match {
                case Option1 => Ok(disclaimer2021View(true))
                case Option2 => Ok(disclaimer2019_2020View())
              }
            }

            case (None,_) => Redirect(routes.IndexController.onPageLoad())
          }
      }
  }
*/

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(DisclaimerPage, request.userAnswers))
  }

}
