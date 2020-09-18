/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

object TaxYearDates {

  // scalastyle:off magic.number
  val TAX_YEAR_2019_START_DATE: LocalDate = LocalDate.of(2019, 4, 6)

  val TAX_YEAR_2019_END_DATE: LocalDate = LocalDate.of(2020, 4, 5)

  val TAX_YEAR_2020_START_DATE: LocalDate = LocalDate.of(2020, 4, 6)

  val TAX_YEAR_2020_END_DATE: LocalDate = LocalDate.of(2021, 4, 5)
  // scalastyle:on magic.number

}
