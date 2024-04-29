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

import com.google.inject.{Inject, Singleton}
import models.{ClaimStatus, TaxYearSelection}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject()(configuration: Configuration, val servicesConfig: ServicesConfig) {

  val contactFormServiceIdentifier = "EEWFH"

  private val contactHost = servicesConfig.getConfString("contact-frontend.www", "")

  val platformHost: Option[String] = configuration.getOptional[String]("platform.frontend.host")

  val pertaxFrontendHost: String =
    platformHost.orElse(Some(configuration.get[Service]("microservice.services.pertax-frontend").baseUrl)).get

  val preferencesFrontendHost: String = configuration.get[Service]("microservice.services.preferences-frontend").baseUrl
  val citizenDetailsHost: String = configuration.get[Service]("microservice.services.citizen-details").baseUrl
  val employeeExpensesHost: String = configuration.get[Service]("microservice.services.employee-expenses-frontend").baseUrl
  val taiHost: String = configuration.get[Service]("microservice.services.tai").baseUrl

  lazy val authUrl: String = configuration.get[Service]("auth").baseUrl
  lazy val loginUrl: String = configuration.get[String]("urls.login")
  lazy val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  lazy val p87DigitalFormUrl: String = configuration.get[String]("urls.p87DigitalForm")
  lazy val p87PostalFormUrl: String = configuration.get[String]("urls.p87PostalForm")
  lazy val ivUpliftUrl: String = configuration.get[String]("urls.ivUplift")
  lazy val ivCompletionUrl: String = configuration.get[String]("urls.ivCompletion")
  lazy val ivFailureUrl: String = configuration.get[String]("urls.ivFailure")
  lazy val feedbackSurvey: String = configuration.get[String]("urls.feedbackSurvey")
  val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"
  lazy val hmrcCallChargesUrl: String = configuration.get[String]("urls.hmrcCallCharges")
  lazy val incomeTaxGeneralEnquiriesUrl: String = configuration.get[String]("urls.incomeTaxGeneralEnquiries")

  lazy val otherExpensesId: Int = configuration.get[Int]("otherExpensesId")
  lazy val jobExpenseId: Int = configuration.get[Int]("jobExpenseId")

  lazy val timeoutDialogEnabled: Boolean = configuration.getOptional[Boolean]("timeoutDialog.enabled").getOrElse(true)
  lazy val timeoutDialogTimeout: Int = configuration.getOptional[Int]("timeoutDialog.timeout").getOrElse(900)
  lazy val timeoutDialogTimeoutCountdown: Int = configuration.getOptional[Int]("timeoutDialog.timeoutCountdown").getOrElse(120)

  def taxReliefPerWeek(taxYear: TaxYearSelection): Int =
    configuration.getOptional[Int](s"taxRelief.${taxYear.toString}.poundsPerWeek")
      .getOrElse(configuration.get[Int]("taxRelief.default.poundsPerWeek"))

  def taxReliefMaxPerYear(taxYear: TaxYearSelection): Int =
    configuration.getOptional[Int](s"taxRelief.${taxYear.toString}.maxPerYear")
      .getOrElse(configuration.get[Int]("taxRelief.default.maxPerYear"))

  lazy val scaWrapperEnabled: Boolean = configuration.get[Boolean]("microservice.services.features.sca-wrapper")

  lazy val optimizelyId: String = configuration.getOptional[String]("optimizely.projectId").getOrElse("18916851035")

  lazy val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("microservice.services.features.welsh-translation")

  val collectionName: String = "user-answers"

  val cacheTtl: Int = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  lazy val mergedJourneyEnabled: Boolean = configuration.getOptional[Boolean]("microservice.services.features.merged-journey").getOrElse(false)

  private val employeeExpensesUrl: String = configuration.get[String]("urls.employeeExpensesUrl")
  def mergedJourneyContinueUrl(claimStatus: ClaimStatus): String =
    s"$employeeExpensesUrl/employee-expenses/merged-journey-continue?journey=wfh&status=$claimStatus"

}


