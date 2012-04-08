import scala.xml.Group
import scalariform.formatter.preferences._
import com.mojolly.scalate.ScalatePlugin._

organization := "io.backchat.oauth2"

name := "oauth2-server"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.1"

javacOptions ++= Seq("-Xlint:unchecked", "-source", "1.7", "-Xlint:deprecation")

scalacOptions ++= Seq("-optimize", "-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8", "-P:continuations:enable")

autoCompilerPlugins := true

libraryDependencies ++= Seq(
  compilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.1"),
  compilerPlugin("org.scala-tools.sxr" % "sxr_2.9.0" % "0.2.7")
)

libraryDependencies ++= Seq(
  "org.scalatra"           % "scalatra"           % "2.1.0-SNAPSHOT",
  "org.scalatra"           % "scalatra-auth"      % "2.1.0-SNAPSHOT",
  "org.scalatra"           % "scalatra-scalate"   % "2.1.0-SNAPSHOT",
  "org.scalatra"           % "scalatra-lift-json" % "2.1.0-SNAPSHOT",
  "org.scalatra"           % "scalatra-swagger"   % "2.1.0-SNAPSHOT",
  "org.clapper"           %% "scalasti"           % "0.5.8",
  "org.mindrot"            % "jbcrypt"            % "0.3m",
  "org.scribe"             % "scribe"             % "1.3.0",
  "javax.mail"             % "mail"               % "1.4.5",
  "commons-codec"          % "commons-codec"      % "1.6",
  "commons-validator"      % "commons-validator"  % "1.4.0",
  "org.scalaz"            %% "scalaz-core"        % "6.0.4",
  "com.typesafe.akka"      % "akka-actor"         % "2.0",
  "com.typesafe.akka"      % "akka-testkit"       % "2.0"             % "test",
  "org.fusesource.scalate" % "scalate-jruby"      % "1.5.3",
  "org.fusesource.scalate" % "scalate-markdownj"  % "1.5.3",
  "org.scala-tools.time"  %% "time"               % "0.5",
  "org.scalatra"          %% "scalatra-specs2"    % "2.1.0-SNAPSHOT"  % "test",
  "junit"                  % "junit"              % "4.10"            % "test",
  "ch.qos.logback"         % "logback-classic"    % "1.0.0",
  "org.eclipse.jetty"      % "jetty-webapp"       % "8.1.0.v20120127",
  "org.eclipse.jetty"      % "jetty-servlet"      % "8.1.0.v20120127",
  "org.eclipse.jetty"      % "test-jetty-servlet" % "8.1.0.v20120127" % "test",
  "org.eclipse.jetty"      % "jetty-annotations"  % "8.1.0.v20120127",
  "com.novus"             %% "salat-core"         % "0.0.8-SNAPSHOT"
)

resolvers += "sonatype oss snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += Classpaths.typesafeResolver

testOptions += Tests.Setup( () => System.setProperty("akka.mode", "test") )

testOptions += Tests.Argument(TestFrameworks.Specs2, "console", "junitxml")

testOptions <+= (crossTarget map { ct =>
 Tests.Setup { () => System.setProperty("specs2.junit.outDir", new File(ct, "specs-reports").getAbsolutePath) }
})

seq(jrebelSettings: _*)

jrebel.webLinks <+= (sourceDirectory in Compile)(_ / "webapp")

homepage := Some(url("https://github.com/backchat/oauth2-server"))

startYear := Some(2010)

licenses := Seq(("MIT", url("https://github.com/backchat/oauth2-server/raw/HEAD/LICENSE")))

pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
  <scm>
    <connection>scm:git:git://github.com/backchat/oauth2-server.git</connection>
    <developerConnection>scm:git:git@github.com:backchat/oauth2-server.git</developerConnection>
    <url>https://github.com/backchat/oauth2-server</url>
  </scm>
  <developers>
    <developer>
      <id>casualjim</id>
      <name>Ivan Porto Carrero</name>
      <url>http://flanders.co.nz/</url>
    </developer>
    <developer>
      <id>sdb</id>
      <name>Stefan De Boey</name>
      <url>http://ellefant.be/</url>
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
  "import io.backchat.oauth2._",
  "import OAuth2Imports._",
  "import model._"
)

scalateBindings ++= Seq(
  Binding("flash", "scala.collection.Map[String, Any]", defaultValue = "Map.empty"),
  Binding("session", "org.scalatra.Session"),
  Binding("sessionOption", "scala.Option[org.scalatra.Session]"),
  Binding("params", "scala.collection.Map[String, String]"),
  Binding("multiParams", "org.scalatra.MultiParams"),
  Binding("userOption", "Option[ResourceOwner]", defaultValue = "None"),
  Binding("user", "ResourceOwner", defaultValue = "null"),
  Binding("isAnonymous", "Boolean", defaultValue = "true"),
  Binding("isAuthenticated", "Boolean", defaultValue = "false"))

seq(coffeeSettings: _*)

(CoffeeKeys.iced in (Compile, CoffeeKeys.coffee)) := true

(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (sourceDirectory in Compile)(_ / "webapp" / "js")