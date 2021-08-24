import BuildHelper._

inThisBuild(
  List(
    organization := "dev.zio",
    homepage := Some(url("https://zio.github.io/zio-prometheus/")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "pshemass",
        "Przemyslaw Wierzbicki",
        "rzbikson@gmail.com",
        url("https://github.com/pshemass")
      )
    ),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc")
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("fix", "; all compile:scalafix test:scalafix; all scalafmtSbt scalafmtAll")
addCommandAlias("check", "; scalafmtSbtCheck; scalafmtCheckAll; compile:scalafix --check; test:scalafix --check")

val zioVersion        = "1.0.10"
val prometheusVersion = "0.10.0"

lazy val root = project
  .in(file("."))
  .settings(
    skip in publish := true,
    unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
  )
  .aggregate(
    zioPrometheusJVM,
    zioPrometheusJS
  )

lazy val zioPrometheus = crossProject(JSPlatform, JVMPlatform)
  .in(file("zio-prometheus"))
  .settings(stdSettings("zio-prometheus"))
  .settings(crossProjectSettings)
  .settings(buildInfoSettings("zio.prometheus"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio"      %% "zio"                     % zioVersion,
      "dev.zio"      %% "izumi-reflect"           % "2.0.0",
      "io.prometheus" % "simpleclient"            % prometheusVersion,
      "io.prometheus" % "simpleclient_httpserver" % prometheusVersion,
      "io.prometheus" % "simpleclient_hotspot"    % prometheusVersion,
      "dev.zio"      %% "zio-test"                % zioVersion % "test",
      "dev.zio"      %% "zio-test-sbt"            % zioVersion % "test"
    )
  )
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))

lazy val zioPrometheusJS = zioPrometheus.js
  .settings(scalaJSUseMainModuleInitializer := true)

lazy val zioPrometheusJVM = zioPrometheus.jvm
  .settings(dottySettings)

lazy val docs = project
  .in(file("zio-prometheus-docs"))
  .settings(
    skip.in(publish) := true,
    moduleName := "zio-prometheus-docs",
    scalacOptions -= "-Yno-imports",
    scalacOptions -= "-Xfatal-warnings",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion
    ),
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(root),
    target in (ScalaUnidoc, unidoc) := (baseDirectory in LocalRootProject).value / "website" / "static" / "api",
    cleanFiles += (target in (ScalaUnidoc, unidoc)).value,
    docusaurusCreateSite := docusaurusCreateSite.dependsOn(unidoc in Compile).value,
    docusaurusPublishGhpages := docusaurusPublishGhpages.dependsOn(unidoc in Compile).value
  )
  .dependsOn(root)
  .enablePlugins(MdocPlugin, DocusaurusPlugin, ScalaUnidocPlugin)
