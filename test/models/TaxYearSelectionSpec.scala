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

import generators.ModelGenerators
import models.TaxYearSelection.CurrentYearMinus4
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, Json}

class TaxYearSelectionSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks with OptionValues with ModelGenerators {

  "TaxYearSelection" must {
    "deserialise valid values" in {
      val gen = arbitrary[TaxYearSelection]

      forAll(gen) {
        selectTaxYearsToClaimFor =>
          Json.toJson(selectTaxYearsToClaimFor.toTaxYear.startYear).validate[TaxYearSelection].asOpt.value mustEqual selectTaxYearsToClaimFor
      }
    }
    "fail to deserialise invalid values" in {
      val invalidYear = CurrentYearMinus4.toTaxYear.back(1).startYear

      intercept[IllegalArgumentException](Json.toJson(invalidYear).validate[TaxYearSelection])
    }
    "deserialise valid list of values while discarding invalid values (in case of tax year rollover)" in {
      val gen = arbitrary[Seq[TaxYearSelection]]
      val invalidYear = CurrentYearMinus4.toTaxYear.back(1).startYear

      forAll(gen) {
        taxYearSeq =>
          Json.toJson(taxYearSeq.map(_.toTaxYear.startYear) :+ invalidYear).validate[Seq[TaxYearSelection]].asOpt.value mustEqual taxYearSeq
      }
    }

    "serialise" in {
      val gen = arbitrary[TaxYearSelection]

      forAll(gen) {
        selectTaxYearsToClaimFor =>
          Json.toJson(selectTaxYearsToClaimFor) mustEqual Json.toJson(selectTaxYearsToClaimFor.toTaxYear.startYear)
      }
    }
  }
}
