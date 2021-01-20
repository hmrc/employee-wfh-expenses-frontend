/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json, JsonValidationError, OFormat, Reads, Writes, __}

import scala.util.{Success, Try}

case class ETag(version: Int)

object ETag {
  implicit lazy val reads: Reads[ETag] = (__ \ "etag").read[String]
    .map(x => Try(ETag(x.toInt)))
    .collect(JsonValidationError("parse error")) {
      case Success(value) => value
    }

  implicit lazy val writes: Writes[ETag] = (__ \ "etag").write[ETag]

  implicit val format: Format[ETag] = Format(reads, writes)
}
