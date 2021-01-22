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

package models.auditing

import models.WithName

sealed trait AuditEventType

object AuditEventType {

  object UpdateWorkingFromHomeFlatRateSuccess extends WithName("UpdateWorkingFromHomeFlatRateSuccess") with AuditEventType

  object UpdateWorkingFromHomeFlatRateFailure extends WithName("UpdateWorkingFromHomeFlatRateFailure") with AuditEventType

  object PaperlessPreferenceCheckSuccess extends WithName("PaperlessPreferenceCheckSuccess") with AuditEventType

  object PaperlessPreferenceCheckFailure extends WithName("PaperlessPreferenceCheckFailure") with AuditEventType

  object AlreadyClaimedExpenses extends WithName("AlreadyClaimedExpenses") with AuditEventType
}

