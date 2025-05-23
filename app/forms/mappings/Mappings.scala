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

package forms.mappings

import play.api.data.FieldMapping
import play.api.data.Forms.of
import models.Enumerable

trait Mappings extends Formatters with Constraints {

  protected def text(errorKey: String = "error.required"): FieldMapping[String] =
    of(stringFormatter(errorKey))

  protected def int(
      requiredKey: String = "error.required",
      wholeNumberKey: String = "error.wholeNumber",
      nonNumericKey: String = "error.nonNumeric",
      args: Seq[String] = Nil
  ): FieldMapping[Int] =
    of(intFormatter(requiredKey, wholeNumberKey, nonNumericKey, args))

  protected def boolean(requiredKey: String, args: Any*): FieldMapping[Boolean] =
    of(booleanFormatter(requiredKey, args: _*))

  protected def enumerable[A](requiredKey: String = "error.required", invalidKey: String = "error.invalid")(
      implicit ev: Enumerable[A]
  ): FieldMapping[A] =
    of(enumerableFormatter[A](requiredKey, invalidKey))

}
