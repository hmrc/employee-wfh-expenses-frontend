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
import pages.{DisclaimerPage, WhenDidYouFirstStartWorkingFromHomePage}
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
                                    ) extends FrontendBaseController with I18nSupport with UIAssembler {

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>

      val selectedTaxYears = taxYearFromUIAssemblerFromRequest()
      val startDate: Option[LocalDate] = request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage)

      def buildDisclaimerPageSettings(dateList: List[(LocalDate, LocalDate)]) = {
        if (request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage).isDefined) {
          DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(dateList), Some(DateLanguageTokenizer.convertList(dateList)))))
        } else {
          DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(dateList), None)))
        }
      }

      Ok(disclaimerView(showBackLink = false, buildDisclaimerPageSettings(selectedTaxYears.assemble), startDate))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(Seq(DisclaimerPage), request.userAnswers))
  }
}