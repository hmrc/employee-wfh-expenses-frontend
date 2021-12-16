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

package navigation

import controllers.routes
import models._
import pages._
import play.api.Logging
import play.api.mvc.Call
import utils.DateLanguageTokenizer

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject()() extends Logging {

  private val normalRoutes: Page => UserAnswers => Call = {
    case ClaimedForTaxYear2020 => ua => claimJourneyFlow(ua)
    case SelectTaxYearsToClaimForPage => ua => startDateJourneyFlow(ua)
    //case SelectTaxYearsToClaimForPage => _ => routes.DisclaimerController.onPageLoad()
    case DisclaimerPage => ua => disclaimerNextPage(ua)
    case WhenDidYouFirstStartWorkingFromHomePage => ua => checkStartWorkingFromHomeDate(ua)
    case _ => _ => routes.IndexController.onPageLoad()
  }

  def nextPage(page: Page, userAnswers: UserAnswers): Call = normalRoutes(page)(userAnswers)

  def startDateJourneyFlow(userAnswers: UserAnswers): Call = {
    if (userAnswers.get(SelectTaxYearsToClaimForPage).get.contains(SelectTaxYearsToClaimFor.Option2)) {
      routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad()
    } else {
      routes.DisclaimerController.onPageLoad()
    }
  }

  def checkStartWorkingFromHomeDate(userAnswers: UserAnswers): Call = {
    val earliestStartDate = LocalDate.of(2020,1,1)

    userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) match {
      case Some(startDate) =>
        startDate.isBefore(earliestStartDate) match {
          case true   => routes.CannotClaimUsingThisServiceController.onPageLoad()
          case false  => routes.DisclaimerController.onPageLoad()
        }
      case None => routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad()
    }

  }

  def claimJourneyFlow(userAnswers: UserAnswers): Call = {
    userAnswers.get(HasSelfAssessmentEnrolment) match {
      case None         => routes.IndexController.onPageLoad()
      case Some(true)   => routes.DisclaimerController.onPageLoad()
      case Some(false)  =>
        userAnswers.get(ClaimedForTaxYear2020) match {
          case Some(claimedAlready) if claimedAlready   => routes.DisclaimerController.onPageLoad()
          case Some(claimedAlready) if !claimedAlready  => routes.SelectTaxYearsToClaimForController.onPageLoad()
          case None                                     => routes.IndexController.onPageLoad()
        }
    }
  }

  def disclaimerNextPage_old(userAnswers: UserAnswers): Call = {
    (
      userAnswers.is2021Only,
      userAnswers.is2019And2020Only,
      userAnswers.is2019And2020And2021Only
    ) match {
      case (true, _, _) => {
        routes.YourTaxReliefController.onPageLoad()
      }
      case (_, true, _) => {
        routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad()
      }
      case (_, _, true) => {
        routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad()
      }
      case (a, b, c) =>
        logger.error(s"Unexpected case match ($a,$b,$c)")
        routes.TechnicalDifficultiesController.onPageLoad()
    }
  }

  def disclaimerNextPage(userAnswers: UserAnswers): Call = {

        routes.YourTaxReliefController.onPageLoad()

    //    logger.error(s"Unexpected case match ($a,$b,$c)")
      //  routes.TechnicalDifficultiesController.onPageLoad()

  }
}
