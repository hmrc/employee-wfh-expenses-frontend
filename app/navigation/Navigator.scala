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

import java.time.LocalDate
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
    case WhenDidYouFirstStartWorkingFromHomePage => ua => checkStartWorkingFromHomeDate(ua)
    case _ => _ => routes.IndexController.onPageLoad()
  }

  def nextPage(page: Page, userAnswers: UserAnswers): Call = normalRoutes(page)(userAnswers)

  def checkYourClaimPage(userAnswers: UserAnswers): Call = {
    routes.DisclaimerController.onPageLoad()
  }

  def checkStartWorkingFromHomeDate(userAnswers: UserAnswers): Call = {
    val earliestStartDate = LocalDate.of(2020,1,1)

    userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) match {
      case Some(startDate) =>
        if (startDate.isBefore(earliestStartDate)) {
          routes.CannotClaimUsingThisServiceController.onPageLoad()
        } else {
          routes.CheckYourClaimController.onPageLoad()
        }
      case None => routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad()
    }

  }

  def claimJourneyFlow(userAnswers: UserAnswers): Call = {
    (userAnswers.get(ClaimedForTaxYear2020),
      userAnswers.get(ClaimedForTaxYear2021),
      userAnswers.get(ClaimedForTaxYear2022),
      userAnswers.get(ClaimedForTaxYear2023)) match {
      case (Some(_), Some(_), Some(_), Some(_)) => routes.SelectTaxYearsToClaimForController.onPageLoad()
      case (None, None, None, None) => routes.IndexController.onPageLoad()
      case (_, _, _, _) => routes.TechnicalDifficultiesController.onPageLoad
    }
  }

  def howWeWillCalculateTaxReliefNextPage(userAnswers: UserAnswers): Call = {
    val selectedTaxYears = userAnswers.get(SelectTaxYearsToClaimForPage).getOrElse(SelectTaxYearsToClaimFor.valuesAll.toSet)

    if(selectedTaxYears.contains(Option1)) {routes.InformClaimNowInWeeksController.onPageLoad()}
    else if(selectedTaxYears.contains(Option4)) {routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad()}
    else {routes.CheckYourClaimController.onPageLoad()}
    // TODO: Will need updating to include the extra tax year when it is added
  }

  def confirmClaimInWeeksNextPage(userAnswers: UserAnswers): Call = {
    val selectedTaxYears = userAnswers.get(SelectTaxYearsToClaimForPage).getOrElse(SelectTaxYearsToClaimFor.valuesAll.toSet)
    val onwardRoute = if(selectedTaxYears.contains(Option4)) {
      routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad()
    } else {
      routes.CheckYourClaimController.onPageLoad()
    }

    if(userAnswers.get(ConfirmClaimInWeeksPage).getOrElse(false)) {
      onwardRoute
    } else {
      routes.NumberOfWeeksToClaimForController.onPageLoad()
    }
  }

}
