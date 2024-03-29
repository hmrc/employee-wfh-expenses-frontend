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

package utils

import java.time.format.DateTimeFormatter

import controllers.routes
import models.UserAnswers
import pages._
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import utils.CheckYourAnswersHelper._
import viewmodels.AnswerRow

class CheckYourAnswersHelper(userAnswers: UserAnswers)(implicit messages: Messages) {

  def selectTaxYearsToClaimFor: Option[AnswerRow] = userAnswers.get(SelectTaxYearsToClaimForPage) map {
    x =>
      AnswerRow(
        HtmlFormat.escape(messages("selectTaxYearsToClaimFor.checkYourAnswersLabel")),
        Html(x.map(value => HtmlFormat.escape(messages(s"selectTaxYearsToClaimFor.$value")).toString).mkString(",<br>")),
        routes.SelectTaxYearsToClaimForController.onPageLoad().url
      )
  }

  def whenDidYouFirstStartWorkingFromHome: Option[AnswerRow] = userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) map {
    x =>
      AnswerRow(
        HtmlFormat.escape(messages("whenDidYouFirstStartWorkingFromHome.checkYourAnswersLabel")),
        HtmlFormat.escape(x.date.format(dateFormatter)),
        routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad().url
      )
  }
}

object CheckYourAnswersHelper {

  private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
}
