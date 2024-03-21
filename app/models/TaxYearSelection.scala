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
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

sealed trait TaxYearSelection

object TaxYearSelection extends Enumerable.Implicits {

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

}
