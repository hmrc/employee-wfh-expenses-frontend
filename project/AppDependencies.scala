import sbt._

object AppDependencies {

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "org.reactivemongo" %% "play2-reactivemongo"            % "0.20.2-play27",
    "uk.gov.hmrc"       %% "logback-json-logger"            % "4.9.0",
    "uk.gov.hmrc"       %% "play-health"                    % "3.16.0-play-27",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"  % "1.5.0-play-27",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-27"     % "3.3.0",
    "uk.gov.hmrc"       %% "play-frontend-govuk"            % "0.60.0-play-27",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"             % "0.37.0-play-27",
    "uk.gov.hmrc"       %% "play-partials"                  % "7.1.0-play-27",
    "uk.gov.hmrc"       %% "tax-year"                       % "1.2.0"
  )

  val test = Seq(
    "org.scalatestplus.play"      %% "scalatestplus-play"   % "4.0.3",
    "org.pegdown"                  % "pegdown"              % "1.6.0",
    "org.jsoup"                    % "jsoup"                % "1.13.1",
    "org.mockito"                  % "mockito-all"          % "1.10.19",
    "org.scalacheck"              %% "scalacheck"           % "1.14.0",
    "com.github.tomakehurst"       % "wiremock-standalone"  % "2.26.3"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
