import sbt._

object AppDependencies {

  val bootstrapVersion = "7.19.0"
  val mongoPlayVersion = "1.3.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"         %% "play-conditional-form-mapping"  % "1.13.0-play-28",
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-play-28"             % mongoPlayVersion,
    "uk.gov.hmrc"         %% "play-conditional-form-mapping"  % "1.12.0-play-28",
    "uk.gov.hmrc"         %% "bootstrap-frontend-play-28"     % bootstrapVersion,
    "uk.gov.hmrc"         %% "tax-year"                       % "3.2.0",
    "uk.gov.hmrc"         %% "play-frontend-hmrc"             % "7.13.0-play-28",
    "com.digitaltangible" %% "play-guard"                     % "2.5.0",
    "uk.gov.hmrc"         %% "sca-wrapper"                    % "1.0.37"

  )

  val test = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-28"    % mongoPlayVersion,
    "org.scalatestplus.play"      %% "scalatestplus-play"         % "5.1.0",
    "org.scalatestplus"           %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2",
    "org.mockito"                  % "mockito-core"               % "5.3.1",
    "org.scalatestplus"           %% "scalatestplus-mockito"      % "1.0.0-M2",
    "org.jsoup"                    % "jsoup"                      % "1.13.1"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
