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

package config

import play.api.{ConfigLoader, Configuration}

import scala.language.implicitConversions

final case class RateLimitConfig(bucketSize: Int, ratePerSecond: Int, enabled: Boolean)

object RateLimitConfig {
  val DEFAULT_SIZE = 5

  implicit lazy val configLoader: ConfigLoader[RateLimitConfig] = ConfigLoader {
    config =>
      prefix =>

        val service  = Configuration(config).get[Configuration](prefix)
        val bucketSize      = service.getOptional[Int]("bucketSize").getOrElse(DEFAULT_SIZE)
        val ratePerSecond   = service.getOptional[Int]("ratePerSecond").getOrElse(DEFAULT_SIZE)
        val enabled         = service.getOptional[Boolean]("enabled").getOrElse(false)

        RateLimitConfig(bucketSize, ratePerSecond, enabled)
  }
}


