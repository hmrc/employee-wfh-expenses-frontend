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

import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.time.TaxYear

import java.time.format.DateTimeFormatter
import scala.util.Try

sealed trait TaxYearSelection {
  def toTaxYear: TaxYear

  def formattedTaxYearArgs(implicit messages: Messages): Seq[String] = {
    val taxYear = toTaxYear
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", messages.lang.toLocale)
    val start: String = taxYear.starts.format(formatter)
    val end: String = taxYear.finishes.format(formatter)

    Seq(start, end)
  }

  override def toString: String = toTaxYear.startYear.toString
}

object TaxYearSelection {
  case object NextYear extends TaxYearSelection {
    def toTaxYear: TaxYear = TaxYear.current.forwards(1)
  }
  case object CurrentYear extends TaxYearSelection {
    def toTaxYear: TaxYear = TaxYear.current
  }
  case object CurrentYearMinus1 extends TaxYearSelection {
    def toTaxYear: TaxYear = TaxYear.current.back(1)
  }
  case object CurrentYearMinus2 extends TaxYearSelection {
    def toTaxYear: TaxYear = TaxYear.current.back(2)
  }
  case object CurrentYearMinus3 extends TaxYearSelection {
    def toTaxYear: TaxYear = TaxYear.current.back(3)
  }
  case object CurrentYearMinus4 extends TaxYearSelection {
    def toTaxYear: TaxYear = TaxYear.current.back(4)
  }

  val valuesAll: Seq[TaxYearSelection] = Seq(
    CurrentYear,
    CurrentYearMinus1,
    CurrentYearMinus2,
    CurrentYearMinus3,
    CurrentYearMinus4
  )

  def mapping: TaxYear => TaxYearSelection = {
    val currentYear = TaxYear.current
    val currentYearMinus1 = TaxYear.current.back(1)
    val currentYearMinus2 = TaxYear.current.back(2)
    val currentYearMinus3 = TaxYear.current.back(3)
    val currentYearMinus4 = TaxYear.current.back(4)

    {
      case `currentYear` => CurrentYear
      case `currentYearMinus1` => CurrentYearMinus1
      case `currentYearMinus2` => CurrentYearMinus2
      case `currentYearMinus3` => CurrentYearMinus3
      case `currentYearMinus4` => CurrentYearMinus4
      case otherYear => throw new IllegalArgumentException(s"Invalid tax year selected: ${otherYear.startYear}")
    }
  }

  def optTaxYearSelection(taxYear: TaxYear): Option[TaxYearSelection] = Try(mapping(taxYear)).toOption

  implicit val reads: Reads[TaxYearSelection] = Reads { json =>
    json
      .validate[Int]
      .map(intYear => mapping(TaxYear(intYear)))
  }
  implicit val writes: Writes[TaxYearSelection] = Writes { taxYearSelection =>
    Json.toJson(taxYearSelection.toTaxYear.startYear)
  }
  implicit val seqReads: Reads[Seq[TaxYearSelection]] = Reads { json =>
    json
      .validate[Seq[Int]]
      .map(_.flatMap(intYear => optTaxYearSelection(TaxYear(intYear))))
  }

  def options(form: Form[_], values: Seq[TaxYearSelection])(implicit messages: Messages): Seq[CheckboxItem] = values.map {
    value =>
      CheckboxItem(
        name = Some("value[]"),
        id = Some(value.toString),
        value = value.toString,
        content = Text(messages(s"selectTaxYearsToClaimFor.${if (value == CurrentYear) "current" else "previous"}", value.formattedTaxYearArgs: _*)),
        checked = form.data.exists(_._2 == value.toString)
      )
  }

  def getClaimableTaxYears(claimedYears: Seq[Int]): Seq[TaxYearSelection] = {
    valuesAll.diff(claimedYears.flatMap(year => optTaxYearSelection(TaxYear(year))))
  }

  def containsCurrent(selectedTaxYears: Seq[TaxYearSelection]): Boolean =
    selectedTaxYears.contains(CurrentYear)

  def containsPrevious(selectedTaxYears: Seq[TaxYearSelection]): Boolean =
    selectedTaxYears.contains(CurrentYearMinus1) ||
      selectedTaxYears.contains(CurrentYearMinus2) ||
      selectedTaxYears.contains(CurrentYearMinus3) ||
      selectedTaxYears.contains(CurrentYearMinus4)

  //TODO : Remove logic for whole year tax claim after 6th April 2027
  def wholeYearClaims: Seq[TaxYearSelection] = Seq(
    optTaxYearSelection(TaxYear(2020)),
    optTaxYearSelection(TaxYear(2021)),
    optTaxYearSelection(TaxYear(2022))
  ).flatten

  def oldGuidanceYears: Seq[TaxYearSelection] =
    Seq(optTaxYearSelection(TaxYear(2020)), optTaxYearSelection(TaxYear(2021))).flatten

  def contains2020or2021(selectedTaxYears: Seq[TaxYearSelection]): Boolean =
    selectedTaxYears.intersect(oldGuidanceYears).nonEmpty

  def contains2022orAfter(selectedTaxYears: Seq[TaxYearSelection]): Boolean =
    selectedTaxYears.diff(oldGuidanceYears).nonEmpty

}
