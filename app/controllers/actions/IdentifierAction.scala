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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject() (
    override val authConnector: AuthConnector,
    config: FrontendAppConfig,
    val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  object LT200 {

    def unapply(confLevel: ConfidenceLevel): Option[ConfidenceLevel] =
      if (confLevel.level < ConfidenceLevel.L200.level) Some(confLevel) else None

  }

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()
      .retrieve(internalId.and(nino).and(affinityGroup).and(confidenceLevel)) {
        case _ ~ _ ~ Some(AffinityGroup.Agent) ~ _ =>
          Future.successful(unauthorisedRoute)
        case _ ~ _ ~ Some(AffinityGroup.Individual | AffinityGroup.Organisation) ~ LT200(_) =>
          Future.successful(upliftConfidenceLevel)
        case mayBeId ~ mayBeNino ~ _ ~ _ =>
          block(
            IdentifierRequest(
              request,
              mayBeId.getOrElse(throw new UnauthorizedException("Unable to retrieve internalId")),
              mayBeNino.getOrElse(throw new UnauthorizedException("Unable to retrieve nino"))
            )
          )
      }
  }.recover {
    case _: NoActiveSession =>
      Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
    case _: InsufficientConfidenceLevel =>
      upliftConfidenceLevel
    case _: AuthorisationException =>
      unauthorisedRoute
  }

  def upliftConfidenceLevel: Result =
    Redirect(
      s"${config.ivUpliftUrl}?origin=EEWFH&confidenceLevel=200" +
        s"&completionURL=${config.ivCompletionUrl}" +
        s"&failureURL=${config.ivFailureUrl}"
    )

  def unauthorisedRoute: Result =
    Redirect(routes.UnauthorisedController.onPageLoad)

}
