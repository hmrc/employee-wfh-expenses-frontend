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

package utils

import java.time.LocalDate

case class DateLanguageTokenizerFormattedItem(day: Int, month: String, year: Int)

object DateLanguageTokenizer {

  type DateLanguageTokenizerFormattedItemTuple = (DateLanguageTokenizerFormattedItem, DateLanguageTokenizerFormattedItem)

  def convertList(dateTupleList: List[(LocalDate, LocalDate)]): List[DateLanguageTokenizerFormattedItemTuple] = {
    dateTupleList.map(dateTuple => convertDate(dateTuple._1, dateTuple._2))
  }

  def convertDate(fromDate: LocalDate, toDate: LocalDate): DateLanguageTokenizerFormattedItemTuple = {
    val from = DateLanguageTokenizerFormattedItem(fromDate.getDayOfMonth, s"month.${fromDate.getMonthValue}", fromDate.getYear)
    val to = DateLanguageTokenizerFormattedItem(toDate.getDayOfMonth, s"month.${toDate.getMonthValue}", toDate.getYear)
    (from, to)
  }

  def convertDate(fromDate: LocalDate): DateLanguageTokenizerFormattedItem = {
    DateLanguageTokenizerFormattedItem(fromDate.getDayOfMonth, s"month.${fromDate.getMonthValue}", fromDate.getYear)
  }
}
