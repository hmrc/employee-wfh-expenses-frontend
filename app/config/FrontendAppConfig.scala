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

package config

import com.google.inject.{Inject, Singleton}
import controllers.routes
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call

@Singleton
class FrontendAppConfig @Inject()(configuration: Configuration) {

  val contactFormServiceIdentifier = "EEWFH"

  val platformHost: Option[String] = configuration.getOptional[String]("platform.frontend.host")

  val pertaxFrontendHost: String =
    platformHost.orElse(Some(configuration.get[Service]("microservice.services.pertax-frontend").baseUrl)).get

  val preferencesFrontendHost: String = configuration.get[Service]("microservice.services.preferences-frontend").baseUrl
  val citizenDetailsHost: String = configuration.get[Service]("microservice.services.citizen-details").baseUrl
  val taiHost: String = configuration.get[Service]("microservice.services.tai").baseUrl

  lazy val authUrl: String = configuration.get[Service]("auth").baseUrl
  lazy val loginUrl: String = configuration.get[String]("urls.login")
  lazy val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  lazy val p87DigitalFormUrl: String = configuration.get[String]("urls.p87DigitalForm")
  lazy val personalDetailsUrl: String = configuration.get[String]("urls.personalDetails")
  lazy val ivUpliftUrl: String = configuration.get[String]("urls.ivUplift")
  lazy val ivCompletionUrl: String = configuration.get[String]("urls.ivCompletion")
  lazy val ivFailureUrl: String = configuration.get[String]("urls.ivFailure")
  lazy val feedbackSurvey: String = configuration.get[String]("urls.feedbackSurvey")

  lazy val otherExpensesId: Int = configuration.get[Int]("otherExpensesId")

  lazy val taxReliefPerWeek2019: Int = configuration.getOptional[Int]("taxRelief.2019.poundsPerWeek").getOrElse(4)
  lazy val taxReliefPerWeek2020: Int = configuration.getOptional[Int]("taxRelief.2020.poundsPerWeek").getOrElse(6)
  lazy val taxReliefMaxPerYear2019: Int = configuration.getOptional[Int]("taxRelief.2019.maxPerYear").getOrElse(212)
  lazy val taxReliefMaxPerYear2020: Int = configuration.getOptional[Int]("taxRelief.2020.maxPerYear").getOrElse(312)

  lazy val timeoutDialogEnabled: Boolean = configuration.get[Boolean]("timeoutDialog.enabled")
  lazy val timeoutDialogTimeout: Int = configuration.get[Int]("timeoutDialog.timeout")
  lazy val timeoutDialogTimeoutCountdown: Int = configuration.get[Int]("timeoutDialog.timeoutCountdown")

  lazy val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("microservice.services.features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call =
    (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)
}
