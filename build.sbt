import scala.xml.Group
import com.typesafe.startscript.StartScriptPlugin
import scalariform.formatter.preferences._
//import RequireJsKeys._
import ScalateKeys._
import Wro4jKeys._
import net.liftweb.json._
import JsonDSL._

organization := "org.scalatra.oauth2"

name := "oauth2-server"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.1"

javacOptions ++= Seq("-Xlint:unchecked", "-source", "1.7", "-target", "1.7", "-Xlint:deprecation")

scalacOptions ++= Seq("-optimize", "-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8", "-P:continuations:enable")

autoCompilerPlugins := true

libraryDependencies ++= Seq(
  compilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.1"),
  compilerPlugin("org.scala-tools.sxr" % "sxr_2.9.0" % "0.2.7")
)

seq(webSettings:_*)

libraryDependencies ++= Seq(
  "org.scalatra"            % "scalatra"             % "2.1.0-SNAPSHOT",
  "org.scalatra"            % "scalatra-auth"        % "2.1.0-SNAPSHOT",
  "org.scalatra"            % "scalatra-scalate"     % "2.1.0-SNAPSHOT",
  "org.scalatra"            % "scalatra-lift-json"   % "2.1.0-SNAPSHOT",
  "org.scalatra"            % "scalatra-swagger"     % "2.1.0-SNAPSHOT",
  "org.scalatra"            % "scalatra-slf4j"       % "2.1.0-SNAPSHOT",
  "org.scalatra"            % "contrib-validation"   % "1.0.5-SNAPSHOT",
  "net.databinder"          % "dispatch-http_2.9.1"  % "0.8.8",
  "net.databinder"          % "dispatch-oauth_2.9.1" % "0.8.8",
  "org.clapper"             % "scalasti_2.9.1"       % "0.5.8",
  "org.mindrot"             % "jbcrypt"              % "0.3m",
  "org.scribe"              % "scribe"               % "1.3.1",
  "javax.mail"              % "mail"                 % "1.4.5",
  "commons-codec"           % "commons-codec"        % "1.6",
  "ro.isdc.wro4j"           % "wro4j-core"           % "1.4.7" exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12"),
  "ro.isdc.wro4j"           % "wro4j-extensions"     % "1.4.7" exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12"),
  "com.typesafe.akka"       % "akka-actor"           % "2.0.2",
  "com.typesafe.akka"       % "akka-testkit"         % "2.0.2"               % "test",
  "org.fusesource.scalate"  % "scalate-markdownj"    % "1.5.3",
  "org.scala-tools.time"    % "time_2.9.1"           % "0.5",
  "org.scalatra"            % "scalatra-specs2"      % "2.1.0-SNAPSHOT"      % "test",
  "junit"                   % "junit"                % "4.10"                % "test",
  "ch.qos.logback"          % "logback-classic"      % "1.0.6",
  "org.eclipse.jetty"       % "jetty-webapp"         % "8.1.3.v20120416"     % "container",
  "org.eclipse.jetty.orbit" % "javax.servlet"        % "3.0.0.v201112011016" % "container;compile" artifacts(Artifact("javax.servlet", "orbit", "jar")),
  "javax.servlet"           % "javax.servlet-api"    % "3.0.1"               % "container;provided",
  "com.novus"               % "salat_2.9.1"          % "1.9.0"
)

resolvers += "sonatype oss snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "sonatype oss releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += Classpaths.typesafeResolver

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

testOptions += Tests.Setup( () => System.setProperty("akka.mode", "test") )

testOptions += Tests.Argument(TestFrameworks.Specs2, "console", "junitxml")

testOptions <+= (crossTarget map { ct =>
 Tests.Setup { () => System.setProperty("specs2.junit.outDir", new File(ct, "specs-reports").getAbsolutePath) }
})

seq(jrebelSettings: _*)

jrebel.webLinks <+= (sourceDirectory in Compile)(_ / "webapp")

homepage := Some(url("https://github.com/scalatra/oauth2-server"))

startYear := Some(2010)

licenses := Seq(("MIT", url("https://github.com/scalatra/oauth2-server/raw/HEAD/LICENSE")))

pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
  <scm>
    <connection>scm:git:git://github.com/scalatra/oauth2-server.git</connection>
    <developerConnection>scm:git:git@github.com:scalatra/oauth2-server.git</developerConnection>
    <url>https://github.com/scalatra/oauth2-server</url>
  </scm>
  <developers>
    <developer>
      <id>casualjim</id>
      <name>Ivan Porto Carrero</name>
      <url>http://flanders.co.nz/</url>
    </developer>
  </developers>
)}

packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "Simple Build Tool",
        "Built-By" -> System.getProperty("user.name"),
        "Build-Jdk" -> System.getProperty("java.version"),
        "Specification-Title" -> title,
        "Specification-Version" -> version,
        "Specification-Vendor" -> vendor,
        "Implementation-Title" -> title,
        "Implementation-Version" -> version,
        "Implementation-Vendor-Id" -> vendor,
        "Implementation-Vendor" -> vendor
      )
  }

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { x => false }

seq(scalariformSettings: _*)

ScalariformKeys.preferences :=
  (FormattingPreferences()
        setPreference(IndentSpaces, 2)
        setPreference(AlignParameters, false)
        setPreference(AlignSingleLineCaseStatements, true)
        setPreference(DoubleIndentClassDeclaration, true)
        setPreference(RewriteArrowSymbols, true)
        setPreference(PreserveSpaceBeforeArguments, true)
        setPreference(IndentWithTabs, false))

(excludeFilter in ScalariformKeys.format) <<= excludeFilter(_ || "*Spec.scala")

seq(scalateSettings:_*)

scalateTemplateDirectory in Compile <<= (baseDirectory) { _ / "src/main/webapp/WEB-INF" }

scalateImports ++= Seq(
  "import scalaz._",
  "import Scalaz._",
  "import org.scalatra.oauth2._",
  "import OAuth2Imports._",
  "import model._"
)

scalateBindings ++= Seq(
  Binding("flash", "scala.collection.Map[String, Any]", defaultValue = "Map.empty"),
  Binding("session", "org.scalatra.servlet.RichSession"),
  Binding("sessionOption", "scala.Option[org.scalatra.servlet.RichSession]"),
  Binding("params", "scala.collection.Map[String, String]"),
  Binding("multiParams", "org.scalatra.MultiParams"),
  Binding("userOption", "Option[Account]", defaultValue = "None"),
  Binding("user", "Account", defaultValue = "null"),
  Binding("isAnonymous", "Boolean", defaultValue = "true"),
  Binding("isAuthenticated", "Boolean", defaultValue = "false"))

seq(buildInfoSettings: _*)

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[Scoped](name, version, scalaVersion, sbtVersion)
// buildInfoKeys := Seq[Scoped](name, version, scalaVersion, sbtVersion)

buildInfoPackage := "org.scalatra.oauth2"

seq(StartScriptPlugin.startScriptForWarSettings: _*)

externalResolvers <<= resolvers map { Resolver.withDefaultResolvers(_, scalaTools = false) }

watchSources <++= (sourceDirectory in Compile) map (d => (d / "webapp" ** "*").get)

//watchSources in Compile <<= (sourceDirectory in Compile in requireJs) map { d => (d ** "*").get }

seq(wro4jSettings: _*)

compile in Compile <<= (compile in Compile).dependsOn(generateResources in Compile)

(webappResources in Compile) <+= (targetFolder in generateResources in Compile)

outputFolder in (Compile, generateResources) := "assets/"

processorProvider in (Compile, generateResources) := new OAuth2Processors

wroFile in (Compile, generateResources) <<= (baseDirectory)(_ / "project" / "wro.xml")

propertiesFile in (Compile, generateResources) <<= (baseDirectory)(_ / "project" / "wro.properties")