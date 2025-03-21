import sbt.Keys.{publishArtifact, _}
import sbt._

val buildScalaVersion = "2.12.3"
val buildCrossScalaVersions = Seq(buildScalaVersion) // Seq("2.11.11", "2.12.2")

val owner = "eidolon-llc"
val repo = "webby"

val baseSettings =  Seq(
  organization := "com.github.citrum.webby",
  version := "0.7.9.5",

  incOptions := incOptions.value.withNameHashing(nameHashing = true),
  resolvers ++= Seq(
    "zeroturnaround repository" at "https://repos.zeroturnaround.com/nexus/content/repositories/zt-public/", // The zeroturnaround.com repository
    "GitHub Package Registry eidolon-llc" at "https://maven.pkg.github.com/eidolon-llc/_"
  ),

  sources in doc in Compile := List(), // Disable generation JavaDoc, ScalaDoc
  mainClass in Compile := None,
  ivyLoggingLevel := UpdateLogging.DownloadOnly,

  // Deploy settings
  startYear := Some(2016),
  homepage := Some(url("https://github.com/citrum/webby")),
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  publishTo := Some("GitHub Package Registry" at s"https://maven.pkg.github.com/$owner/$repo"),
  scmInfo := Some(ScmInfo(url(s"https://github.com/$owner/$repo"), s"scm:git@github.com:$owner/$repo.git")),
  publishMavenStyle := true
)

val commonSettings = baseSettings ++ Seq(
  scalaVersion := buildScalaVersion,
  crossScalaVersions := buildCrossScalaVersions,

  scalacOptions ++= Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature", "-language:existentials", "-Xlint:-unused"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8"),
  javacOptions in doc := Seq("-source", "1.8"),
  ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true)) // forcing scala version
)

// Minimal dependencies
val commonDependencies = {
  val deps = Seq.newBuilder[ModuleID]
  deps += "ch.qos.logback" % "logback-classic" % "1.2.3" // Logging
  deps += "org.apache.commons" % "commons-lang3" % "3.6"
  deps += "org.apache.commons" % "commons-text" % "1.1"
  deps += "com.google.guava" % "guava" % "21.0"
  deps += "com.google.code.findbugs" % "jsr305" % "3.0.1" // @Nonnull, @Nullable annotation support
  deps += "commons-io" % "commons-io" % "2.7" // Contains useful classes like FileUtils

  // Tests
  deps += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  deps += "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % "test"

  deps.result()
}
val querio = "com.github.citrum.querio" %% "querio" % "0.7.1" // querio orm

/**
  * Создать список настроек, задающих стандартные пути исходников, ресурсов, тестов для проекта.
  */
def makeSourceDirs(): Seq[Setting[_]] = Seq(
  sourceDirectories in Compile += baseDirectory.value / "src",
  scalaSource in Compile := baseDirectory.value / "src",
  javaSource in Compile := baseDirectory.value / "src",
  resourceDirectory in Compile := baseDirectory.value / "conf",
  scalaSource in Test := baseDirectory.value / "test",
  resourceDirectory in Test := baseDirectory.value / "test-conf")

/**
  * Запустить scala класс кодогенерации в отдельном процессе
  */
def runScala(classPath: Seq[File], className: String, arguments: Seq[String] = Nil) {
  val ret: Int = new Fork("java", Some(className)).apply(ForkOptions(bootJars = classPath, outputStrategy = Some(StdoutOutput)), arguments)
  if (ret != 0) sys.error("Trouble with code generator")
}

// ------------------------------ elastic-orm projects ------------------------------

// Legacy elastic-orm for Elastic v2.x
lazy val elasticOrm2: Project = Project(
  "elastic-orm-2",
  file("elastic-orm-2"),
  settings = Defaults.coreDefaultSettings ++ commonSettings ++ makeSourceDirs() ++ Seq(
    libraryDependencies ++= commonDependencies,
    libraryDependencies += "org.elasticsearch" % "elasticsearch" % "2.4.5" exclude("com.google.guava", "guava"), // Клиент поискового движка (да и сам движок), exclude guava нужен потому что эластик использует более старую версию 18
    libraryDependencies += querio
  )).dependsOn(webby)

// Elastic-orm for Elastic v6.x
lazy val elasticOrm6: Project = Project(
  "elastic-orm-6",
  file("elastic-orm-6"),
  settings = Defaults.coreDefaultSettings ++ commonSettings ++ makeSourceDirs() ++ Seq(
    libraryDependencies ++= commonDependencies,
    libraryDependencies += "org.elasticsearch.client" % "transport" % "6.0.0" exclude("com.google.guava", "guava"), // Клиент поискового движка (да и сам движок)
    libraryDependencies += "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.8.2",   // fixes bug https://discuss.elastic.co/t/issue-with-elastic-search-5-0-0-noclassdeffounderror-org-apache-logging-log4j-logger/64262
    libraryDependencies += querio
  )).dependsOn(webby)

// ------------------------------ webby-haxe project ------------------------------

lazy val webbyHaxe: Project = Project(
  "webby-haxe",
  file("webby-haxe"),
  settings = baseSettings ++ Seq(
    name := "webby-haxe",
    artifactClassifier := Some("haxe"),

    libraryDependencies += "com.github.citrum" % "haxe-jar" % "3.4.4" classifier "haxe",

    unmanagedResourceDirectories in Compile := Seq(baseDirectory.value / "src", baseDirectory.value / "macro"),

    crossPaths := false, // Turn off scala versions
    // Disable javadoc, source generation
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false
  )
)

// ------------------------------ webby-haxe-test project ------------------------------

val npmInstall = taskKey[Unit]("run npm install")
lazy val npmInstallTask = npmInstall := Process("env npm install", file(".")).!!

val haxeTestWithNpmInstall = taskKey[Unit]("run haxeTest with npm install")

lazy val webbyHaxeTest: Project = Project(
  "webby-haxe-test",
  file("webby-haxe-test"),
  settings = makeSourceDirs() ++ Seq(
    scalaVersion := buildScalaVersion,
    // Disable packaging & publishing artifact
    Keys.`package` := file(""),
    publishArtifact := false,
    publishLocal := {},
    publish := {},

    npmInstallTask,
    haxeTestWithNpmInstall := Def.sequential(npmInstall).value,
    test in Test := {val _ = haxeTestWithNpmInstall.value; (test in Test).value},

    //managedHaxeSourceSubDirs in Test ++= (unmanagedResourceDirectories in Compile in webbyHaxe).value,

    libraryDependencies += "com.google.javascript" % "closure-compiler" % "v20170124" exclude("com.google.guava", "guava"), // Google Closure Compiler
    libraryDependencies += "org.clojure" % "google-closure-library" % "0.0-20160609-f42b4a24" // Google Closure Library
  )
).dependsOn(webby).dependsOn(webbyHaxe)


// ------------------------------ webby project ------------------------------

lazy val webby: Project = Project(
  "webby",
  file("webby"),
  settings = commonSettings ++ makeSourceDirs() ++ Seq(
    description := "Webby is a scala web framework",
    libraryDependencies := {
      val deps = Seq.newBuilder[ModuleID]
      deps ++= commonDependencies
      deps += "org.slf4j" % "jul-to-slf4j" % "1.7.25"
      deps += "org.slf4j" % "jcl-over-slf4j" % "1.7.25"

      deps += "io.netty" % "netty-all" % "4.1.44.Final"

      deps += "com.typesafe" % "config" % "1.3.1"

      // Важно! Нельзя повышать версию модуля jackson-module-scala на ветку 2.5, 2.6, 2.7.
      // Это приводит к смене поведения при сериализации. Например, webby.form.jsrule.JsRule
      // перестаёт сериализовывать свойства cond, actions несмотря на аннотации @JsonProperty.
      // Если же не ставить @JsonAutoDetect(getterVisibility = NONE), то сериализация работает, хотя
      // появляются лишние поля.
      deps += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.9" // Работа с json
      deps += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.9" exclude("com.google.guava", "guava") exclude("com.google.code.findbugs", "jsr305") // Работа с json
      deps += "org.jetbrains" % "annotations" % "15.0" // для интеграции IDEA language injection

      // Optional dependencies
      deps += querio % "optional" // Querio ORM
      deps += "com.beachape" %% "enumeratum" % "1.5.12" // Enums
      deps += "com.typesafe.akka" %% "akka-actor" % "2.4.17" % "optional" // Used in webby.api.libs.concurrent.Akka
      deps += "com.typesafe.akka" %% "akka-slf4j" % "2.4.17" % "optional"
      deps += "org.scala-stm" %% "scala-stm" % "0.8" % "optional" // Used in webby.api.libs.concurrent.Promise
      deps += "com.zaxxer" % "HikariCP" % "2.6.3" % "optional" // Database connector, used in webby.api.db.HikariCPPlugin
      deps += "org.jsoup" % "jsoup" % "1.10.3" % "optional" // Html parsing, used in webby.commons.text.StdStrHtmlJsoup
      deps += "org.zeroturnaround" % "jr-sdk" % "6.4.6" % "optional" // JRebel SDK (class reloader), used in webby.commons.system.JRebelUtils
      deps += "uk.co.caprica" % "juds" % "0.94.1" % "optional" // Unix socket support, used in webby.commons.system.SdDaemon
      deps += "javax.servlet" % "javax.servlet-api" % "3.1.0" % "optional" // Servlet api for dump Sentry client
      deps += "io.sentry" % "sentry-logback" % "1.6.3" % "optional" // Sentry plugin for log processing. Used in webby.commons.system.log.SentryFilteredAppender
      deps += "commons-validator" % "commons-validator" % "1.6" % "optional" intransitive() // Email validation, used in webby.commons.text.validator.EmailValidator
      deps += "org.apache.commons" % "commons-email" % "1.4" % "optional" // Email classes, used in webby.commons.text.validator.EmailValidator
      deps += "org.quartz-scheduler" % "quartz" % "2.2.3" % "optional" exclude("c3p0", "c3p0") // Cron, used in webby.commons.system.cron.BaseQuartzPlugin
      deps += "commons-codec" % "commons-codec" % "1.10" % "optional"
      deps += "net.sf.ehcache" % "ehcache-core" % "2.6.11" % "optional" // Cache, used in webby.commons.cache.CachePlugin
      deps += "com.esotericsoftware.kryo" % "kryo" % "2.24.0" % "optional" // For serializing objects in cache, used in webby.commons.cache.KryoNamedCache
      deps += "com.carrotsearch" % "hppc" % "0.7.2" % "optional" // High Performance Primitive Collections, used in ElasticSearch & in webby.commons.cache.IntIntPositiveValueMap
      deps += "com.google.javascript" % "closure-compiler" % "v20170124" % "optional" exclude("com.google.guava", "guava") // Google Closure Compiler
      deps += "org.clojure" % "google-closure-library" % "0.0-20160609-f42b4a24" % "optional" // Google Closure Library
      deps += "org.glassfish.external" % "opendmk_jmxremote_optional_jar" % "1.0-b01-ea" % "optional" // JMXMP - better replacement for RMI
      deps += "org.apache.httpcomponents" % "httpclient" % "4.5.3" % "optional" // Used in ReCaptcha
      deps += "io.bit3" % "jsass" % "5.5.3" % "optional" // Used in webby.mvc.script.compiler.LibSassCompiler
      deps.result()
    },
    javacOptions ++= Seq("-XDenableSunApiLintControl"),
    scalacOptions ++= Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-Xlint", "-Xlint:-nullary-unit") // nullary-unit нужен только для оператора def > : Unit в CommonTag
  )
)

lazy val root = Project(
  "webby-root",
  file("."),
  aggregate = Seq(webby, elasticOrm2, elasticOrm6, webbyHaxe, webbyHaxeTest),
  settings = Seq(
    // Disable packaging & publishing artifact
    Keys.`package` := file(""),
    publishArtifact := false,
    publishLocal := {},
    publish := {},

    // Наводим красоту в командной строке sbt
    shellPrompt := {state: State => "[" + scala.Console.GREEN + "webby" + scala.Console.RESET + "] "}
  ))
