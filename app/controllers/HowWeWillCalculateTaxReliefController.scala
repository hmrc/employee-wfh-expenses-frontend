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

import actions._
import javax.inject.Inject
import navigation.Navigator
import pages._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TaxYearFormatter
import views.html.HowWeWillCalculateTaxReliefView

class HowWeWillCalculateTaxReliefController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      identify: IdentifierAction,
                                      citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      navigator: Navigator,
                                      val controllerComponents: MessagesControllerComponents,
                                      howWeWillCalculateTaxReliefView: HowWeWillCalculateTaxReliefView
                                    ) extends FrontendBaseController with I18nSupport with UIAssembler {

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(SelectTaxYearsToClaimForPage) match {
        case Some(_) =>
          val selectedTaxYearsAssembler = taxYearFromUIAssemblerFromRequest()

          val taxYears = selectedTaxYearsAssembler.assemble
          val formattedTaxYears = TaxYearFormatter(taxYears).formattedTaxYears

          Ok(howWeWillCalculateTaxReliefView(formattedTaxYears))

        case None => Redirect(routes.IndexController.start)
      }

  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(HowWeWillCalculateTaxReliefPage, request.userAnswers))
  }
}