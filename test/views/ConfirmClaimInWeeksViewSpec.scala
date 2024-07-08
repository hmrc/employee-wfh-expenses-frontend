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

package views

import forms.ConfirmClaimInWeeksFormProvider
import models.TaxYearSelection
import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1}
import play.api.Application
import play.api.data.Form
import play.twirl.api.Html
import views.behaviours.ViewBehaviours
import views.html.ConfirmClaimInWeeksView

// scalastyle:off magic.number
class ConfirmClaimInWeeksViewSpec extends ViewBehaviours {


  val application: Application = applicationBuilder().build()

  val view: ConfirmClaimInWeeksView = application.injector.instanceOf[ConfirmClaimInWeeksView]
  val form = new ConfirmClaimInWeeksFormProvider()(2)

  def createView(form: Form[Boolean], numberofWeeks: Int, taxYearSelected: TaxYearSelection): Html = view.apply(form, numberofWeeks, taxYearSelected)(fakeRequest, messages)

  "Confirm claim in weeks view" should {

    "have the correct banner title for current tax year" in {
      val doc = asDocument(createView(form, 2, CurrentYear))
      val banner = doc.select(".govuk-header__service-name")

      banner.text() mustEqual messages("service.name")
    }

    "have the correct banner title for previous tax year" in {
      val doc = asDocument(createView(form, 2, CurrentYearMinus1))
      val banner = doc.select(".govuk-header__service-name")

      banner.text() mustEqual messages("service.name")
    }

    "show content" when {
      "when all confirmClaimInWeeks content is displayed for current tax year" in {
        val taxYear = CurrentYear
        val title = "Do you want to claim for 2 weeks of working from home in the current tax year?"
        val hint = s"The claim of 2 weeks is for the current tax year between ${taxYear.formattedTaxYearArgs.head} and ${taxYear.formattedTaxYearArgs.apply(1)}"
        val doc = asDocument(createView(form, 2, taxYear))
        assertContainsMessages(doc, title)
        assertContainsMessages(doc, hint)
      }

      "when all confirmClaimInWeeks content is displayed for previous tax year" in {
        val taxYear = CurrentYearMinus1
        val title = s"Do you want to claim for 2 weeks of working from home between ${taxYear.formattedTaxYearArgs.head} and ${taxYear.formattedTaxYearArgs.apply(1)}?"
        val hint = s"The claim of 2 weeks is for the tax year between ${taxYear.formattedTaxYearArgs.head} and ${taxYear.formattedTaxYearArgs.apply(1)}."
        val doc = asDocument(createView(form, 2, taxYear))
        assertContainsMessages(doc, title)
        assertContainsMessages(doc, hint)
      }
    }

    behave like pageWithBackLink(createView(form, 2, CurrentYear))

  }
}
