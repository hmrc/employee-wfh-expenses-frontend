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

  val valuesWithoutPrev = Seq(Option1, Option2)
  val valuesWithout2021 = Seq(Option1, Option3)
  val valuesWithout2022 = Seq(Option2, Option3)

  val values: Seq[SelectTaxYearsToClaimFor] = Seq(
    Option1,
    Option2,
    Option3
  )

  def options(form: Form[_])(implicit messages: Messages): Seq[CheckboxItem] = values.map {
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
    Enumerable(values.map(v => v.toString -> v): _*)
}
