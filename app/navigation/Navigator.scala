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

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import pages._
import models._

@Singleton
class Navigator @Inject()() {

  private val normalRoutes: Page => UserAnswers => Call = {
    case WhenDidYouFirstStartWorkingFromHomePage => ua => checkStartWorkingFromHomeDate(ua)
    case IndexPage => ua => claimJourneyFlow(ua)
    case _ => _ => routes.IndexController.onPageLoad()
  }

  def nextPage(page: Page, userAnswers: UserAnswers): Call = normalRoutes(page)(userAnswers)

  def checkStartWorkingFromHomeDate(userAnswers: UserAnswers) = {
    val earliestStartDate = LocalDate.of(2019,4,6)

    userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) match {
      case Some(startDate) =>
        startDate.isBefore(earliestStartDate) match {
          case true   => routes.CannotClaimUsingThisServiceController.onPageLoad()
          case false  => routes.YourTaxReliefController.onPageLoad()
        }
      case None => routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad()
    }
  }

  def claimJourneyFlow(userAnswers: UserAnswers) = {
    userAnswers.get(IndexPage) match {
      case Some(claimedAlready) if claimedAlready   => routes.DisclaimerController.onPageLoad() // top flow on iteration 5 diagram, just 2021-22 to claim
      case Some(claimedAlready) if !claimedAlready  => routes.DisclaimerController.onPageLoad() // show checkbox to select which years to claim
      case None                                     => routes.IndexController.onPageLoad()      // if missing, start again in order to get it this answer
    }
  }
}
