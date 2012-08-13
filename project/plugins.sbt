scalacOptions += "-deprecation"

resolvers += "Jawsy.fi M2 releases" at "http://oss.jawsy.fi/maven2/releases"

resolvers += Resolver.url("sbt-plugin-snapshots",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots/"))(
    Resolver.ivyStylePatterns)

resolvers += Classpaths.typesafeResolver

//resolvers += "Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases"

// addSbtPlugin("com.typesafe.startscript" % "xsbt-start-script-plugin" % "0.5.2")

addSbtPlugin("fi.jawsy.sbtplugins" %% "sbt-jrebel-plugin" % "0.9.0")

addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.0.10")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.1.2")

//addSbtPlugin("org.scalatra.requirejs" % "sbt-requirejs" % "0.0.3")

//addSbtPlugin("io.backchat.sbtbrew" % "brew" % "0.1.0-SNAPSHOT")

addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.2.1")

libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-web-plugin" % (v+"-0.2.11.1"))

libraryDependencies += "net.liftweb" % "lift-json_2.9.1" % "2.4"

addSbtPlugin("com.bowlingx" % "xsbt-wro4j-plugin" % "0.1.0-SNAPSHOT")

resolvers += "scct-repo" at "http://mtkopone.github.com/scct/maven-repo/"

addSbtPlugin("com.eed3si9n" % "sbt-unique-version" % "latest.integration")

addSbtPlugin("com.github.casualjim" % "sbt-jelastic-deploy" % "0.1.2")