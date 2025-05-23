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
import models.TaxYearSelection.{contains2020or2021, contains2022orAfter}

import javax.inject.Inject
import navigation.Navigator
import pages.{DisclaimerPage, SelectTaxYearsToClaimForPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DisclaimerView

class DisclaimerController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    navigator: Navigator,
    val controllerComponents: MessagesControllerComponents,
    disclaimerView: DisclaimerView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    identify.andThen(citizenDetailsCheck).andThen(getData).andThen(requireData) { implicit request =>
      request.userAnswers.get(SelectTaxYearsToClaimForPage) match {
        case Some(selectedTaxYears) =>
          Ok(disclaimerView(contains2022orAfter(selectedTaxYears), contains2020or2021(selectedTaxYears)))
        case None => Redirect(routes.IndexController.start)
      }
    }

  def onSubmit(): Action[AnyContent] = identify.andThen(citizenDetailsCheck).andThen(getData).andThen(requireData) {
    implicit request => Redirect(navigator.nextPage(DisclaimerPage, request.userAnswers))
  }

}
