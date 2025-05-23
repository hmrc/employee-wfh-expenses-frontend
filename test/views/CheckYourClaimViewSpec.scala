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
import models.TaxYearSelection.{
  CurrentYear,
  CurrentYearMinus1,
  CurrentYearMinus2,
  CurrentYearMinus3,
  CurrentYearMinus4,
  NextYear,
  wholeYearClaims
}
import play.api.test.FakeRequest
import views.behaviours.ViewBehaviours
import views.html.CheckYourClaimView

import java.time.format.DateTimeFormatter
import scala.collection.immutable.ListMap

// scalastyle:off magic.number
class CheckYourClaimViewSpec extends ViewBehaviours {

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  val groupedSelectedTaxYears: Seq[(Int, Seq[TaxYearSelection])] =
    List(
      (10, List(CurrentYear, CurrentYearMinus1)),
      (9, List(CurrentYearMinus2, CurrentYearMinus3)),
      (8, List(CurrentYearMinus4))
    )

  val weeksCurrent       = 1
  val weeksCurrentMinus1 = 41
  val weeksCurrentMinus2 = 42
  val weeksCurrentMinus3 = 43
  val weeksCurrentMinus4 = 44

  var weeksForTaxYears: ListMap[TaxYearSelection, Int] = ListMap[TaxYearSelection, Int](
    CurrentYear       -> weeksCurrent,
    CurrentYearMinus1 -> weeksCurrentMinus1
  )

  if (!wholeYearClaims.contains(CurrentYearMinus2)) weeksForTaxYears += (CurrentYearMinus2 -> weeksCurrentMinus2)
  if (!wholeYearClaims.contains(CurrentYearMinus3)) weeksForTaxYears += (CurrentYearMinus3 -> weeksCurrentMinus3)
  if (!wholeYearClaims.contains(CurrentYearMinus4)) weeksForTaxYears += (CurrentYearMinus4 -> weeksCurrentMinus4)

  object ExpectedContent {
    val title       = "Check and submit your claim"
    val heading     = "Check and submit your claim"
    val subheading1 = "Check your claim details"
    val text1_1     = "You are entitled to tax relief on"
    val text1_2     = "a week on your expenses for working from home for:"

    def claimContent(taxYear: TaxYearSelection, numOfWeeks: Option[Int]): String =
      if (numOfWeeks.nonEmpty) {
        s"${numOfWeeks.get} ${if (numOfWeeks.get == 1) "week" else "weeks"} of tax year ${taxYear.toTaxYear.starts.format(formatter)}" +
          s" to ${taxYear.toTaxYear.finishes.format(formatter)}"
      } else {
        s"the whole of tax year ${taxYear.toTaxYear.starts.format(formatter)}" +
          s" to ${taxYear.toTaxYear.finishes.format(formatter)}"
      }

    val text2: String = s"At the end of this tax year, the tax relief will stop. If you are required to work from" +
      s" home from the ${NextYear.toTaxYear.starts.format(formatter)}, you will need to claim again."

    val detailsHeading = "What to do if your extra costs are more than this"

    val details1 =
      "If your extra costs are more than this you may be able to claim for more tax relief. You will need to"

    val detailsLink = "claim tax relief for expenses of employment"
    val details2    = "and provide evidence, like detailed and verifiable receipts and bills."
    val subheading2 = "Now submit your claim"

    val text3 =
      "By submitting this claim you are confirming that, to the best of your knowledge, the details you are providing are correct."

  }

  "Check your claim page" must {
    import ExpectedContent._

    val request = FakeRequest()

    val view         = viewFor[CheckYourClaimView](Some(emptyUserAnswers))
    val renderedView = view(groupedSelectedTaxYears, weeksForTaxYears, currentYearContent = true)(request, messages)
    val doc          = asDocument(renderedView)

    behave.like(
      normalPage(
        renderedView,
        "checkYourClaimView",
        args = Nil
      )
    )

    "have the correct title and heading" in {
      assertContainsText(doc, title)
      assertContainsText(doc, heading)
    }
    "have the correct content" in {
      assertContainsText(doc, subheading1)
      assertContainsText(doc, text1_1)
      assertContainsText(doc, text1_2)
      assertContainsText(doc, text2)
      assertContainsText(doc, subheading2)
      assertContainsText(doc, text3)
    }
    "have the correct list content" in {
      assertContainsText(doc, claimContent(CurrentYear, Some(weeksCurrent)))
      assertContainsText(doc, claimContent(CurrentYearMinus1, Some(weeksCurrentMinus1)))
      assertContainsText(
        doc,
        claimContent(
          CurrentYearMinus2,
          if (!wholeYearClaims.contains(CurrentYearMinus2)) Some(weeksCurrentMinus2) else None
        )
      )
      assertContainsText(
        doc,
        claimContent(
          CurrentYearMinus3,
          if (!wholeYearClaims.contains(CurrentYearMinus3)) Some(weeksCurrentMinus3) else None
        )
      )
      assertContainsText(
        doc,
        claimContent(
          CurrentYearMinus4,
          if (!wholeYearClaims.contains(CurrentYearMinus4)) Some(weeksCurrentMinus4) else None
        )
      )
    }
    "have the correct details content" in {
      assertContainsText(doc, detailsHeading)
      assertContainsText(doc, details1)
      assertContainsText(doc, detailsLink)
      assertContainsText(doc, details2)
    }
    "not have the need to claim again text when current year is not selected" in {
      val view = viewFor[CheckYourClaimView](Some(emptyUserAnswers))
      val renderedView = view(
        groupedSelectedTaxYears,
        weeksForTaxYears.filterNot(_._1.equals(CurrentYear)),
        currentYearContent = false
      )(request, messages)
      val doc = asDocument(renderedView)

      assertNotContainsText(doc, text2)
    }
  }

}
