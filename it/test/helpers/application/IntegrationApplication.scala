/*
 * Copyright 2026 HM Revenue & Customs
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

package helpers.application

import helpers.wiremock.WireMockConfig
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

trait IntegrationApplication extends GuiceOneServerPerSuite with WireMockConfig {
  self: TestSuite =>

  def appConfig(extraConfig: (String, String)*): Map[String, Any] = Map(
    "microservice.services.bas-gateway.host" -> wireMockHost,
    "microservice.services.bas-gateway.port" -> wireMockPort,
    "microservice.services.citizen-details.host" -> wireMockHost,
    "microservice.services.citizen-details.port" -> wireMockPort,
    "microservice.services.employee-expenses-frontend.host" -> wireMockHost,
    "microservice.services.employee-expenses-frontend.port" -> wireMockPort,
    "microservice.services.tai.host" -> wireMockHost,
    "microservice.services.tai.port" -> wireMockPort,
    "microservice.services.preferences-frontend.host" -> wireMockHost,
    "microservice.services.preferences-frontend.port" -> wireMockPort,
    "microservice.services.citizen-details.host" -> wireMockHost,
    "microservice.services.citizen-details.port" -> wireMockPort,
    "otherExpensesId" -> 59,
    "jobExpenseId"    -> 55
  ) ++ extraConfig

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(appConfig())
    .build()
}
