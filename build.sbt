
inThisBuild(Seq(
  name := "prometheus-datadog-bridge",
  organization := "com.github.chris_zen",

  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  homepage := Some(url(s"https://github.com/chris-zen/${name.value}")),
  developers := List(Developer("chris-zen", "Christian Perez-Llamas", "chrispz@gmail.com", url("http://chris-zen.github.io/"))),
  scmInfo := Some(ScmInfo(url(s"https://github.com/chris-zen/${name.value}"), s"scm:git:git@github.com:chris-zen/${name.value}.git")),

  // These are the sbt-release-early settings to configure
  pgpPublicRing := file("./travis/local.pubring.asc"),
  pgpSecretRing := file("./travis/local.secring.asc"),
  releaseEarlyWith := BintrayPublisher,
  releaseEarlyEnableSyncToMaven := false,

  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.4")
))

lazy val IntegrationTest = config("it") extend Test

lazy val itSettings = inConfig(IntegrationTest)(Defaults.testSettings) ++ Seq(
  fork in IntegrationTest := false,
  parallelExecution in IntegrationTest := false
)

lazy val root = Project(id = "prometheus-datadog-bridge", base = file("."))
  .configs(IntegrationTest)
  .settings(itSettings)
  .settings(
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "1.7.25",
      "io.prometheus" % "simpleclient" % "0.6.0",
      "com.datadoghq" % "java-dogstatsd-client" % "2.7",

      "org.slf4j" % "slf4j-log4j12" % "1.7.25" % Test,
      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "org.mockito" % "mockito-core" % "2.12.0" % Test,
    )
  )
