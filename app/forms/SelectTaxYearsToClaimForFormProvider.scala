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
import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, seq}
import uk.gov.hmrc.time.TaxYear

import javax.inject.Inject
import scala.util.Try

class SelectTaxYearsToClaimForFormProvider @Inject() extends Mappings {

  val errorKey = "selectTaxYearsToClaimFor.error.required"

  def apply(): Form[Seq[TaxYearSelection]] = Form(
    "value" -> seq(text(errorKey))
      .verifying(errorKey, _.nonEmpty)
      .verifying(
        errorKey,
        _.forall(yearInt => Try(TaxYearSelection.mapping(TaxYear(yearInt.toInt))).toOption.nonEmpty)
      )
      .transform[Seq[TaxYearSelection]](
        _.map(year => TaxYearSelection.mapping(TaxYear(year.toInt))),
        _.map(_.toString)
      )
  )
}
