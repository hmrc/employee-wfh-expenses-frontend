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

package forms

import forms.mappings.Mappings
import models.TaxYearSelection
import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1, CurrentYearMinus2, CurrentYearMinus3, CurrentYearMinus4}
import play.api.data.Forms.{ignored, mapping}
import play.api.data.{Form, Mapping}
import play.api.i18n.Messages

import javax.inject.Inject
import scala.collection.immutable.ListMap

class NumberOfWeeksToClaimForFormProvider @Inject() extends Mappings {

  val ONE_WEEK = 1
  val MAXIMUM_WEEKS_IN_A_YEAR = 52

  def apply(selectedTaxYears: Seq[TaxYearSelection])(implicit messages: Messages): Form[ListMap[TaxYearSelection, Int]] = {
    def errorPrefix(taxYear: TaxYearSelection): String = if (taxYear.equals(CurrentYear)) {
      "numberOfWeeksToClaimFor.error"
    } else {
      "numberOfWeeksToClaimFor.previous.error"
    }

    def weekMapping(taxYear: TaxYearSelection): (String, Mapping[Int]) =
      taxYear.toString -> {
        if (selectedTaxYears.contains(taxYear)) {
          int(
            s"${errorPrefix(taxYear)}.required",
            s"${errorPrefix(taxYear)}.wholeNumber",
            s"${errorPrefix(taxYear)}.nonNumeric",
            taxYear.formattedTaxYearArgs
          ).verifying(minimumValue(ONE_WEEK, s"${errorPrefix(taxYear)}.minimum", taxYear.formattedTaxYearArgs))
            .verifying(maximumValue(MAXIMUM_WEEKS_IN_A_YEAR, s"${errorPrefix(taxYear)}.maximum", taxYear.formattedTaxYearArgs))
        } else {
          ignored[Int](0)
        }
      }

    Form(
      mapping(
        weekMapping(CurrentYear),
        weekMapping(CurrentYearMinus1),
        weekMapping(CurrentYearMinus2),
        weekMapping(CurrentYearMinus3),
        weekMapping(CurrentYearMinus4)
      )((cty, ctyMinus1, ctyMinus2, ctyMinus3, ctyMinus4) =>
        ListMap[TaxYearSelection, Int](
          CurrentYear -> cty,
          CurrentYearMinus1 -> ctyMinus1,
          CurrentYearMinus2 -> ctyMinus2,
          CurrentYearMinus3 -> ctyMinus3,
          CurrentYearMinus4 -> ctyMinus4
        ).filter(_._2 > 0)
      )(weekMap =>
        Some(
          weekMap.getOrElse(CurrentYear, 0),
          weekMap.getOrElse(CurrentYearMinus1, 0),
          weekMap.getOrElse(CurrentYearMinus2, 0),
          weekMap.getOrElse(CurrentYearMinus3, 0),
          weekMap.getOrElse(CurrentYearMinus4, 0)
        )
      )
    )
  }
}
