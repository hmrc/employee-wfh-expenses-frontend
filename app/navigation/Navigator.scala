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

package navigation

import controllers.routes
import models.SelectTaxYearsToClaimFor._
import models._
import pages._
import play.api.Logging
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject()() extends Logging {

  private val normalRoutes: Page => UserAnswers => Call = {
    case ClaimedForTaxYear2020 => ua => claimJourneyFlow(ua)
    case SelectTaxYearsToClaimForPage => _ => routes.DisclaimerController.onPageLoad()
    case DisclaimerPage => _ => routes.HowWeWillCalculateTaxReliefController.onPageLoad()
    case HowWeWillCalculateTaxReliefPage => ua => howWeWillCalculateTaxReliefNextPage(ua)
    case InformClaimNowInWeeksPage => _ => routes.NumberOfWeeksToClaimForController.onPageLoad()
    case NumberOfWeeksToClaimForPage => _ => routes.ConfirmClaimInWeeksController.onPageLoad()
    case ConfirmClaimInWeeksPage => ua => confirmClaimInWeeksNextPage(ua)
    case CheckYourClaimPage => ua => checkYourClaimPage(ua)
    case _ => _ => routes.IndexController.start
  }

  def nextPage(page: Page, userAnswers: UserAnswers): Call = normalRoutes(page)(userAnswers)

  def checkYourClaimPage(userAnswers: UserAnswers): Call = {
    routes.DisclaimerController.onPageLoad()
  }

  def claimJourneyFlow(userAnswers: UserAnswers): Call = {
    (userAnswers.get(ClaimedForTaxYear2020),
      userAnswers.get(ClaimedForTaxYear2021),
      userAnswers.get(ClaimedForTaxYear2022),
      userAnswers.get(ClaimedForTaxYear2023),
      userAnswers.get(ClaimedForTaxYear2024)) match {
      case (Some(_), Some(_), Some(_), Some(_), Some(_)) => routes.SelectTaxYearsToClaimForController.onPageLoad()
      case (None, None, None, None, None) => routes.IndexController.start
      case (_, _, _, _, _) => routes.TechnicalDifficultiesController.onPageLoad
    }
  }

  def howWeWillCalculateTaxReliefNextPage(userAnswers: UserAnswers): Call = {
    val selectedTaxYears = userAnswers.get(SelectTaxYearsToClaimForPage).getOrElse(SelectTaxYearsToClaimFor.valuesAll)

    if(selectedTaxYears.contains(Option2)) {routes.InformClaimNowInWeeksController.onPageLoad()}
    else {routes.CheckYourClaimController.onPageLoad()}
    // TODO: Will need updating to include the extra tax year when it is added
  }

  def confirmClaimInWeeksNextPage(userAnswers: UserAnswers): Call = {
    if(userAnswers.get(ConfirmClaimInWeeksPage).getOrElse(false)) {
      routes.CheckYourClaimController.onPageLoad()
    } else {
      routes.NumberOfWeeksToClaimForController.onPageLoad()
    }
  }

}
