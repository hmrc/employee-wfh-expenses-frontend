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
import models.SelectTaxYearsToClaimFor.{Option1, Option2}
import navigation.Navigator
import pages.{ClaimedForTaxYear2020, DisclaimerPage, HasSelfAssessmentEnrolment, SelectTaxYearsToClaimForPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{Disclaimer2019_2020View, Disclaimer2019_2020_2021View, Disclaimer2021View}

import javax.inject.Inject

class DisclaimerController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      identify: IdentifierAction,
                                      citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      navigator: Navigator,
                                      val controllerComponents: MessagesControllerComponents,
                                      disclaimer2021View : Disclaimer2021View,
                                      disclaimer2019_2020View: Disclaimer2019_2020View,
                                      disclaimer2019_2020_2021View: Disclaimer2019_2020_2021View
                                     ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>

      request.userAnswers.get(HasSelfAssessmentEnrolment) match {
        case None        => Redirect(routes.IndexController.onPageLoad())
        case Some(true)  => Ok(disclaimer2021View())
        case Some(false) =>
          (request.userAnswers.get(ClaimedForTaxYear2020), request.userAnswers.get(SelectTaxYearsToClaimForPage) ) match {
            case (Some(true), _)      => Ok(disclaimer2021View())

            case (Some(false), None)  => Redirect(routes.SelectTaxYearsToClaimForController.onPageLoad())

            case (Some(false), Some(yearsToClaimFor)) => yearsToClaimFor.size match {
              case 0 => Redirect(routes.SelectTaxYearsToClaimForController.onPageLoad())
              case 2 => Ok(disclaimer2019_2020_2021View())
              case 1 => yearsToClaimFor.head match {
                case Option1 => Ok(disclaimer2021View())
                case Option2 => Ok(disclaimer2019_2020View())
              }
            }

            case (None,_) => Redirect(routes.IndexController.onPageLoad())
          }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(DisclaimerPage, request.userAnswers))
  }
}
