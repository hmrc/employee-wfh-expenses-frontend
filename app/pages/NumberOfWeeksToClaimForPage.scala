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

package pages

import models.TaxYearSelection
import play.api.libs.json._

case object NumberOfWeeksToClaimForPage extends QuestionPage[Map[TaxYearSelection, Int]] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "numberOfWeeksToClaimForPage"

  val format: Format[Map[TaxYearSelection, Int]] = new Format[Map[TaxYearSelection, Int]] {
    override def reads(json: JsValue): JsResult[Map[TaxYearSelection, Int]] =
      json.validate[Map[TaxYearSelection, Int]]

    override def writes(answerMap: Map[TaxYearSelection, Int]): JsValue =
      Json.toJson(
        answerMap.map { case (taxYear, weeks) =>
          taxYear.toString -> weeks
        }
      )
  }
}
