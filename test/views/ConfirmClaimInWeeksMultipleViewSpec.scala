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

import models.TaxYearSelection
import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1}
import play.api.test.FakeRequest
import views.behaviours.ViewBehaviours
import views.html.ConfirmClaimInWeeksMultipleView

import java.time.format.DateTimeFormatter
import scala.collection.immutable.ListMap

// scalastyle:off magic.number
class ConfirmClaimInWeeksMultipleViewSpec extends ViewBehaviours {

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  val weeksCurrent  = 1
  val weeksPrevious = 43

  val weeksForTaxYears: ListMap[TaxYearSelection, Int] = ListMap[TaxYearSelection, Int](
    CurrentYear       -> weeksCurrent,
    CurrentYearMinus1 -> weeksPrevious
  )

  object ExpectedContent {
    val title   = "Check the number of weeks in your claim for these tax years"
    val heading = "Check the number of weeks in your claim for these tax years"

    val date1: String = s"${CurrentYear.toTaxYear.starts.format(formatter)}" +
      s" to ${CurrentYear.toTaxYear.finishes.format(formatter)}"

    val date2: String = s"${CurrentYearMinus1.toTaxYear.starts.format(formatter)}" +
      s" to ${CurrentYearMinus1.toTaxYear.finishes.format(formatter)}"

    val claim1 = s"$weeksCurrent week"
    val claim2 = s"$weeksPrevious weeks"

    val change1: String =
      s"the number of weeks for the current tax year ${CurrentYear.toTaxYear.starts.format(formatter)}" +
        s" to ${CurrentYear.toTaxYear.finishes.format(formatter)}"

    val change2: String =
      s"the number of weeks for the previous tax year ${CurrentYearMinus1.toTaxYear.starts.format(formatter)}" +
        s" to ${CurrentYearMinus1.toTaxYear.finishes.format(formatter)}"

  }

  "Check your claim page" must {
    import ExpectedContent._

    val request = FakeRequest()

    val view         = viewFor[ConfirmClaimInWeeksMultipleView](Some(emptyUserAnswers))
    val renderedView = view(weeksForTaxYears)(request, messages)
    val doc          = asDocument(renderedView)

    behave.like(
      normalPage(
        renderedView,
        "confirmClaimInWeeks.multiple",
        args = Nil
      )
    )

    "have the correct title and heading" in {
      assertContainsText(doc, title)
      assertContainsText(doc, heading)
    }
    "have the correct table content" in {
      assertContainsText(doc, date1)
      assertContainsText(doc, date2)
      assertContainsText(doc, claim1)
      assertContainsText(doc, claim2)
      assertContainsText(doc, change1)
      assertContainsText(doc, change2)
    }
  }

}
