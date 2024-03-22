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

import forms.behaviours.IntFieldBehaviours
import models.TaxYearSelection.CurrentYear
import org.scalatestplus.play.guice.GuiceFakeApplicationFactory
import play.api.data.FormError
import play.api.i18n.{Lang, Messages, MessagesApi}
import utils.TaxYearDates.{MAXIMUM_WEEKS_IN_A_YEAR, ONE_WEEK}

class NumberOfWeeksToClaimForFormProviderSpec extends IntFieldBehaviours with GuiceFakeApplicationFactory {

  implicit val messages: Messages = fakeApplication().injector.instanceOf[MessagesApi].preferred(Seq(Lang.defaultLang))
  val formProvider: NumberOfWeeksToClaimForFormProvider = new NumberOfWeeksToClaimForFormProvider

  "value" should {
    val form = formProvider(CurrentYear)
    behave like intFieldWithMinimum(
      form,
      "value",
      ONE_WEEK,
      FormError("value", "numberOfWeeksToClaimFor.error.minimum", 1 +: CurrentYear.formattedTaxYearArgs)
    )
    behave like intFieldWithMaximum(
      form,
      "value",
      MAXIMUM_WEEKS_IN_A_YEAR,
      FormError("value", "numberOfWeeksToClaimFor.error.maximum", 52 +: CurrentYear.formattedTaxYearArgs)
    )
  }
}
