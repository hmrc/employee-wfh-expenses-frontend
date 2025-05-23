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

package models.paperless

import play.api.libs.json.{Json, OFormat}

case class PaperlessStatus(
    name: String
)

object PaperlessStatus {
  implicit val format: OFormat[PaperlessStatus] = Json.format[PaperlessStatus]
}

case class Url(
    link: String,
    text: String
)

object Url {
  implicit val format: OFormat[Url] = Json.format[Url]
}

case class PaperlessStatusResponse(
    status: PaperlessStatus,
    url: Url
) {
  def isPaperlessCustomer: Boolean = status.name == "ALRIGHT"
}

object PaperlessStatusResponse {
  implicit val format: OFormat[PaperlessStatusResponse] = Json.format[PaperlessStatusResponse]
}
