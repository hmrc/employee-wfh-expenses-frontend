import sbt._

object AppDependencies {

  val scaWrapperPlayVersion = "6.0.0"
  val mongoPlayVersion      = "2.13.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping-play-30" % "3.5.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"                    % mongoPlayVersion,
    "uk.gov.hmrc"       %% "tax-year"                              % "6.0.0",
    "uk.gov.hmrc"       %% "sca-wrapper-play-30"                   % scaWrapperPlayVersion
  )

  val test = Seq(
    "uk.gov.hmrc"       %% "sca-wrapper-test-play-30" % scaWrapperPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30"  % mongoPlayVersion,
    "org.scalatestplus" %% "scalacheck-1-17"          % "3.2.18.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
