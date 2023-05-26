import sbt._

object AppDependencies {

  val bootstrapVersion = "7.15.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "org.reactivemongo"   %% "play2-reactivemongo"            % "0.19.7-play28",
    "uk.gov.hmrc"         %% "play-conditional-form-mapping"  % "1.13.0-play-28",
    "uk.gov.hmrc"         %% "bootstrap-frontend-play-28"     % bootstrapVersion,
    "uk.gov.hmrc"         %% "play-frontend-hmrc"             % "7.0.0-play-28",
    "uk.gov.hmrc"         %% "play-partials"                  % "8.4.0-play-28",
    "uk.gov.hmrc"         %% "tax-year"                       % "3.2.0",
    "com.digitaltangible" %% "play-guard"                     % "2.5.0"
  )

  val test = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"     % bootstrapVersion,
    "org.scalatestplus.play"      %% "scalatestplus-play"         % "5.1.0",
    "org.scalatestplus"           %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2",
    "org.scalatestplus"           %% "scalatestplus-mockito"      % "1.0.0-M2",
    "org.jsoup"                    % "jsoup"                      % "1.13.1"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
