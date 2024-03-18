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

sealed trait SelectTaxYearsToClaimFor

object SelectTaxYearsToClaimFor extends Enumerable.Implicits {

  case object Option1 extends WithName("option1") with SelectTaxYearsToClaimFor
  case object Option2 extends WithName("option2") with SelectTaxYearsToClaimFor
  case object Option3 extends WithName("option3") with SelectTaxYearsToClaimFor
  case object Option4 extends WithName("option4") with SelectTaxYearsToClaimFor
  case object Option5 extends WithName("option5") with SelectTaxYearsToClaimFor

  val valuesAll: Seq[SelectTaxYearsToClaimFor] = Seq(
    Option1, // 2024-2025 (CY)
    Option2, // 2023-2024 (CY-1)
    Option3, // 2022-2023 (CY-2)
    Option4, // 2021-2022 (CY-3)
    Option5 // 2020-2021 (CY-4)
  )

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
                                   claimed2023: Boolean,
                                   claimed2024: Boolean): Seq[SelectTaxYearsToClaimFor] = {
    Seq(
      if (claimed2024) None else Some(Option1),
      if (claimed2023) None else Some(Option2),
      if (claimed2022) None else Some(Option3),
      if (claimed2021) None else Some(Option4),
      if (claimed2020) None else Some(Option5)
    ).flatten
  }

}
