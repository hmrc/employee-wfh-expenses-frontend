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

package utils

import java.time.LocalDate

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import utils.DateLanguageTokenizer.DateLanguageTokenizerFormattedItemTuple

class DateLanguageTokenizerTest extends SpecBase with MockitoSugar {

  "Formatting date month" should {
    "be translated" when {
      "for each month" in {

        val dateList = (1 to 12).map(month => LocalDate.of(2000, month, month + 1))
          .toList.map(date => (date, date))

        val result: List[DateLanguageTokenizerFormattedItemTuple] = DateLanguageTokenizer.convertList(dateList)

        val expectedResults = (1 to 12).map(month => DateLanguageTokenizerFormattedItem(month + 1, s"month.$month", 2000))
          .toList.map(date => (date, date))

        result mustBe expectedResults
      }
    }
  }
}
