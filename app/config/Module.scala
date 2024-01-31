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

import com.google.inject.{AbstractModule, Provides}
import connectors.{PaperlessPreferenceConnector, PaperlessPreferenceConnectorImpl}
import controllers.actions._
import play.api.{Configuration, Environment}
import services.{IABDService, IABDServiceImpl}
import utils.RateLimiting
import views.templates.{LayoutProvider, NewLayoutProvider, OldLayoutProvider}

import javax.inject.Named

class Module(environment: Environment, config: Configuration) extends AbstractModule {

  val scaWrapperEnabled: Boolean = config.getOptional[Boolean]("microservice.services.features.sca-wrapper").getOrElse(false)

  override def configure(): Unit = {
    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()
    bind(classOf[ManualCorrespondenceIndicatorAction]).to(classOf[ManualCorrespondenceIndicatorActionImpl]).asEagerSingleton()

    // For session based storage instead of cred based, change to SessionIdentifierAction
    bind(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).asEagerSingleton()
    bind(classOf[PaperlessPreferenceConnector]).to(classOf[PaperlessPreferenceConnectorImpl]).asEagerSingleton()

    bind(classOf[IABDService]).to(classOf[IABDServiceImpl]).asEagerSingleton()

    if (scaWrapperEnabled) {
      bind(classOf[LayoutProvider]).to(classOf[NewLayoutProvider]).asEagerSingleton()
    } else {
      bind(classOf[LayoutProvider]).to(classOf[OldLayoutProvider]).asEagerSingleton()
    }

    @Provides
    @Named("IABD GET")
    def rateLimitForIabdGets(appConfig: FrontendAppConfig): RateLimiting = {
      new RateLimiting(appConfig.rateLimitTaiGets, "IABD GET rate limit triggered")
    }

    @Provides
    @Named("IABD POST")
    def rateLimitForIabdPosts(appConfig: FrontendAppConfig): RateLimiting = {
      new RateLimiting(appConfig.rateLimitTaiPosts, "IABD POST rate limited triggered")
    }
  }
}