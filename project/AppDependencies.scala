import sbt._

object AppDependencies {

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "org.reactivemongo"   %% "play2-reactivemongo"            % "0.19.7-play28",
    "uk.gov.hmrc"         %% "logback-json-logger"            % "5.1.0",
    "uk.gov.hmrc"         %% "play-conditional-form-mapping"  % "1.9.0-play-28",
    "uk.gov.hmrc"         %% "bootstrap-frontend-play-28"     % "5.3.0",
    "uk.gov.hmrc"         %% "play-frontend-hmrc"             % "0.72.0-play-28",
    "uk.gov.hmrc"         %% "play-partials"                  % "8.1.0-play-28",
    "uk.gov.hmrc"         %% "tax-year"                       % "1.3.0",
    "com.digitaltangible" %% "play-guard"                     % "2.4.0"
  )

  val test = Seq(
    "org.scalatestplus.play"      %% "scalatestplus-play"         % "5.1.0",
    "org.scalatestplus"           %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2",
    "org.scalatestplus"           %% "scalatestplus-mockito"      % "1.0.0-M2",
    "com.vladsch.flexmark"         %  "flexmark-all"              % "0.35.10",
    "org.pegdown"                  % "pegdown"                    % "1.6.0",
    "org.jsoup"                    % "jsoup"                      % "1.13.1",
    "org.mockito"                  % "mockito-all"                % "1.10.19",
    "org.scalacheck"              %% "scalacheck"                 % "1.15.2",
    "com.github.tomakehurst"       % "wiremock-standalone"        % "2.27.2"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
