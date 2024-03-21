/*
 * Copyright 2024 HM Revenue & Customs
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

package utils

import play.api.i18n.Messages

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class TaxYearFormatter(taxYears: List[(LocalDate, LocalDate)])(implicit val messages: Messages) {

  lazy val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", messages.lang.toLocale)

  def formattedTaxYears: List[(String, String)] = {
    taxYears.map {
      case (startDate, endDate) => (startDate.format(formatter), endDate.format(formatter))
    }
  }
}
