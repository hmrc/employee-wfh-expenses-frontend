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
import models.TaxYearSelection._
import models._
import pages._
import play.api.Logging
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () extends Logging {

  private val normalRoutes: Page => UserAnswers => Call = {
    case ClaimedForTaxYears              => ua => claimJourneyFlow(ua)
    case SelectTaxYearsToClaimForPage    => _ => routes.DisclaimerController.onPageLoad()
    case DisclaimerPage                  => _ => routes.HowWeWillCalculateTaxReliefController.onPageLoad()
    case HowWeWillCalculateTaxReliefPage => ua => howWeWillCalculateTaxReliefNextPage(ua)
    case InformClaimNowInWeeksPage       => _ => routes.NumberOfWeeksToClaimForController.onPageLoad()
    case NumberOfWeeksToClaimForPage     => _ => routes.ConfirmClaimInWeeksController.onPageLoad()
    case ConfirmClaimInWeeksPage         => ua => confirmClaimInWeeksNextPage(ua)
    case _                               => _ => routes.IndexController.start
  }

  def nextPage(page: Page, userAnswers: UserAnswers): Call = normalRoutes(page)(userAnswers)

  def claimJourneyFlow(userAnswers: UserAnswers): Call =
    userAnswers.get(ClaimedForTaxYears) match {
      case Some(_) => routes.SelectTaxYearsToClaimForController.onPageLoad()
      case None    => routes.IndexController.start
    }

  def howWeWillCalculateTaxReliefNextPage(userAnswers: UserAnswers): Call =
    userAnswers.get(SelectTaxYearsToClaimForPage).getOrElse(Nil) match {
      case list if list.diff(TaxYearSelection.wholeYearClaims).nonEmpty =>
        routes.InformClaimNowInWeeksController.onPageLoad()
      case list if list.nonEmpty =>
        routes.CheckYourClaimController.onPageLoad()
      case Nil =>
        routes.IndexController.start
      case _ =>
        val errorMessage = s"ERROR: Unhandled state encountered"
        logger.error(errorMessage)
        throw new IllegalStateException(errorMessage)
    }

  def confirmClaimInWeeksNextPage(userAnswers: UserAnswers): Call =
    if (userAnswers.get(ConfirmClaimInWeeksPage).getOrElse(false)) {
      routes.CheckYourClaimController.onPageLoad()
    } else {
      routes.NumberOfWeeksToClaimForController.onPageLoad()
    }

}
