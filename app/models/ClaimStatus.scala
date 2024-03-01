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

package models

sealed trait ClaimStatus {
  val key: String = toString
}
abstract class ClaimComplete extends ClaimStatus
case object ClaimCompleteCurrent extends ClaimComplete
case object ClaimCompletePrevious extends ClaimComplete
case object ClaimCompleteCurrentPrevious extends ClaimComplete
case object ClaimUnsuccessful extends ClaimStatus