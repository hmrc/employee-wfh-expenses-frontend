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

import com.digitaltangible.ratelimit.RateLimiter
import config.RateLimitConfig
import uk.gov.hmrc.http.TooManyRequestException

class RateLimiting(rateLimitConfig: RateLimitConfig, tooManyRequestsMsg: String = "Rate limit detected") {

  val bucket: RateLimiter = new RateLimiter(rateLimitConfig.bucketSize, rateLimitConfig.ratePerSecond.toFloat)

  val enabled: Boolean = rateLimitConfig.enabled

  def hasAToken: Boolean = bucket.consumeAndCheck("")

  def withToken[T](action: () => T): T = {
    if ( !enabled || hasAToken) {
      action()
    } else {
      throw new TooManyRequestException(tooManyRequestsMsg)
    }
  }
}
