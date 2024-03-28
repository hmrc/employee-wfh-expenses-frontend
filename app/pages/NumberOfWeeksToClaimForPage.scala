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

import scala.collection.immutable.ListMap

case object NumberOfWeeksToClaimForPage extends QuestionPage[ListMap[TaxYearSelection, Int]] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "numberOfWeeksToClaimForPage"

  implicit val format: Format[ListMap[TaxYearSelection, Int]] = new Format[ListMap[TaxYearSelection, Int]] {
    override def reads(json: JsValue): JsResult[ListMap[TaxYearSelection, Int]] =
      json.validate[List[(TaxYearSelection, Int)]].map(list => ListMap(list: _*))

    override def writes(answerMap: ListMap[TaxYearSelection, Int]): JsValue =
      Json.toJson(
        answerMap.toList
      )
  }
}
