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

package models

import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1, CurrentYearMinus2, CurrentYearMinus3, CurrentYearMinus4, NextYear}
import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed trait TaxYearSelection {
  def toTaxYear: TaxYear = this match {
    case NextYear => TaxYear.current.forwards(1)
    case CurrentYear => TaxYear.current
    case CurrentYearMinus1 => TaxYear.current.back(1)
    case CurrentYearMinus2 => TaxYear.current.back(2)
    case CurrentYearMinus3 => TaxYear.current.back(3)
    case CurrentYearMinus4 => TaxYear.current.back(4)
    case _ => throw new IllegalArgumentException("Invalid tax year selected")
  }

  def formattedTaxYearArgs(implicit messages: Messages): Seq[String] = {
    val taxYear = toTaxYear
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", messages.lang.toLocale)
    val start: String = taxYear.starts.format(formatter)
    val end: String = taxYear.finishes.format(formatter)

    Seq(start, end)
  }
}

object TaxYearSelection extends Enumerable.Implicits {

  case object NextYear extends TaxYearSelection
  case object CurrentYear extends WithName("option1") with TaxYearSelection
  case object CurrentYearMinus1 extends WithName("option2") with TaxYearSelection
  case object CurrentYearMinus2 extends WithName("option3") with TaxYearSelection
  case object CurrentYearMinus3 extends WithName("option4") with TaxYearSelection
  case object CurrentYearMinus4 extends WithName("option5") with TaxYearSelection

  val valuesAll: Seq[TaxYearSelection] = Seq(
    CurrentYear, // 2024-2025 (CY)
    CurrentYearMinus1, // 2023-2024 (CY-1)
    CurrentYearMinus2, // 2022-2023 (CY-2)
    CurrentYearMinus3, // 2021-2022 (CY-3)
    CurrentYearMinus4 // 2020-2021 (CY-4)
  )

  def options(form: Form[_], values: Seq[TaxYearSelection])(implicit messages: Messages): Seq[CheckboxItem] = values.map {
    value =>
      CheckboxItem(
        name = Some("value[]"),
        id = Some(value.toString),
        value = value.toString,
        content = Text(messages(s"selectTaxYearsToClaimFor.${value.toString}")),
        checked = form.data.exists(_._2 == value.toString)
      )
  }

  implicit val enumerable: Enumerable[TaxYearSelection] =
    Enumerable(valuesAll.map(v => v.toString -> v): _*)

  def getValuesFromClaimedBooleans(claimed2020: Boolean,
                                   claimed2021: Boolean,
                                   claimed2022: Boolean,
                                   claimed2023: Boolean,
                                   claimed2024: Boolean): Seq[TaxYearSelection] = {
    Seq(
      if (claimed2024) None else Some(CurrentYear),
      if (claimed2023) None else Some(CurrentYearMinus1),
      if (claimed2022) None else Some(CurrentYearMinus2),
      if (claimed2021) None else Some(CurrentYearMinus3),
      if (claimed2020) None else Some(CurrentYearMinus4)
    ).flatten
  }

  def containsCurrent(selectedTaxYears: Seq[TaxYearSelection]): Boolean = selectedTaxYears.contains(CurrentYear)
  def containsPrevious(selectedTaxYears: Seq[TaxYearSelection]): Boolean = selectedTaxYears.contains(CurrentYearMinus1) || selectedTaxYears.contains(CurrentYearMinus2) || selectedTaxYears.contains(CurrentYearMinus3) || selectedTaxYears.contains(CurrentYearMinus4)

  val wholeYearClaims: Seq[TaxYearSelection] = Seq(CurrentYearMinus2, CurrentYearMinus3, CurrentYearMinus4)

  def contains2020or2021(selectedTaxYears: Seq[TaxYearSelection]): Boolean = selectedTaxYears.intersect(Seq(CurrentYearMinus3, CurrentYearMinus4)).nonEmpty

  def contains2022orAfter(selectedTaxYears: Seq[TaxYearSelection]): Boolean = selectedTaxYears.intersect(Seq(CurrentYear, CurrentYearMinus1, CurrentYearMinus2)).nonEmpty

}
