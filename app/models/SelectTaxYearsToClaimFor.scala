/*
 * Copyright 2022 HM Revenue & Customs
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

sealed trait SelectTaxYearsToClaimFor

object SelectTaxYearsToClaimFor extends Enumerable.Implicits {

  case object Option1 extends WithName("option1") with SelectTaxYearsToClaimFor

  case object Option2 extends WithName("option2") with SelectTaxYearsToClaimFor

  case object Option3 extends WithName("option3") with SelectTaxYearsToClaimFor

  case object Option4 extends WithName("option4") with SelectTaxYearsToClaimFor

  val valuesAll: Seq[SelectTaxYearsToClaimFor] = Seq(
    Option1, // 2023-2024 (CY)
    Option2, // 2022-2023 (CY-1)
    Option3, // 2021-2022 (CY-2)
    Option4 // 2020-2021, 2019-2020 (CY-3, CY-4)
  )

  val values2023Only: Seq[SelectTaxYearsToClaimFor] = Seq(Option1)
  val values2022Only: Seq[SelectTaxYearsToClaimFor] = Seq(Option2)
  val values2021Only: Seq[SelectTaxYearsToClaimFor] = Seq(Option3)
  val values2020Only: Seq[SelectTaxYearsToClaimFor] = Seq(Option4)
  val values2023And2022: Seq[SelectTaxYearsToClaimFor] = Seq(Option1, Option2)
  val values2023And2021: Seq[SelectTaxYearsToClaimFor] = Seq(Option1, Option3)
  val values2023And2020: Seq[SelectTaxYearsToClaimFor] = Seq(Option1, Option4)
  val values2022And2021: Seq[SelectTaxYearsToClaimFor] = Seq(Option2, Option3)
  val values2022And2020: Seq[SelectTaxYearsToClaimFor] = Seq(Option2, Option4)
  val values2021And2020: Seq[SelectTaxYearsToClaimFor] = Seq(Option3, Option4)
  val values2023And2022And2021: Seq[SelectTaxYearsToClaimFor] = Seq(Option1, Option2, Option3)
  val values2023And2021And2020: Seq[SelectTaxYearsToClaimFor] = Seq(Option1, Option3, Option4)
  val values2022And2021And2020: Seq[SelectTaxYearsToClaimFor] = Seq(Option2, Option3, Option4)
  val values2023And2022And2020: Seq[SelectTaxYearsToClaimFor] = Seq(Option1, Option2, Option4)

  def options(form: Form[_], values: Seq[SelectTaxYearsToClaimFor])(implicit messages: Messages): Seq[CheckboxItem] = values.map {
    value =>
      CheckboxItem(
        name = Some("value[]"),
        id = Some(value.toString),
        value = value.toString,
        content = Text(messages(s"selectTaxYearsToClaimFor.${value.toString}")),
        checked = form.data.exists(_._2 == value.toString)
      )
  }

  implicit val enumerable: Enumerable[SelectTaxYearsToClaimFor] =
    Enumerable(valuesAll.map(v => v.toString -> v): _*)

  def getValuesFromClaimedBooleans(claimed2020: Boolean,
                                   claimed2021: Boolean,
                                   claimed2022: Boolean,
                                   claimed2023: Boolean): Seq[SelectTaxYearsToClaimFor] = {
    (claimed2023, claimed2022, claimed2021, claimed2020) match {
      case (false, true, true, true) => values2023Only
      case (true, false, true, true) => values2022Only
      case (true, true, false, true) => values2021Only
      case (true, true, true, false) => values2020Only
      case (false, false, true, true) => values2023And2022
      case (false, true, false, true) => values2023And2021
      case (false, true, true, false) => values2023And2020
      case (true, false, false, true) => values2022And2021
      case (true, false, true, false) => values2022And2020
      case (true, true, false, false) => values2021And2020
      case (false, false, false, true) => values2023And2022And2021
      case (false, true, false, false) => values2023And2021And2020
      case (true, false, false, false) => values2022And2021And2020
      case (false, false, true, false) => values2023And2022And2020
      case (_, _, _, _) => valuesAll

    }
  }

}
