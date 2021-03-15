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

import base.SpecBase
import config.RateLimitConfig
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.Matchers.{convertToAnyShouldWrapper, _}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.TooManyRequestException

class RateLimitingSpec extends SpecBase with MockitoSugar {

  "RateLimiting.enabled" should {
    "return true" when {
      "the rate limiter is enabled" in {
        val rateConfig = RateLimitConfig(2, 2, enabled = true)
        new RateLimiting(rateConfig).enabled shouldBe true
      }
    }

    "return false" when {
      "the rate limiter is disabled" in {
        val rateConfig = RateLimitConfig(2, 2, enabled = false)
        new RateLimiting(rateConfig).enabled shouldBe false
      }
    }
  }

  "RateLimiting.withToken" should {

    "not execute the function and throw a TooManyRequestException" when {
      "rate liming enabled and a token is denied" in {
        val rateConfig = RateLimitConfig(2, 2, enabled = true)
        val limiter = Mockito.spy(new RateLimiting(rateConfig))

        when(limiter.enabled).thenReturn(true)
        when(limiter.hasAToken).thenReturn(false)

        intercept[TooManyRequestException] {
          limiter.withToken[String](() => "success")
        }
      }
    }

    "execute the function" when {
      "rate liming enabled and a token is granted" in {
        val rateConfig = RateLimitConfig(2, 2, enabled = true)
        val limiter = Mockito.spy(new RateLimiting(rateConfig))

        when(limiter.enabled).thenReturn(true)
        when(limiter.hasAToken).thenReturn(true)

        limiter.withToken[String](() => "success") shouldBe "success"
      }

      "rate liming disabled and a token is denied" in {
        val rateConfig = RateLimitConfig(2, 2, enabled = false)
        val limiter = Mockito.spy(new RateLimiting(rateConfig))

        when(limiter.enabled).thenReturn(false)
        when(limiter.hasAToken).thenReturn(false)

        limiter.withToken[String](() => "success") shouldBe "success"
      }
    }

  }
}