scalacOptions += "-deprecation"

resolvers += "Jawsy.fi M2 releases" at "http://oss.jawsy.fi/maven2/releases"

resolvers += Resolver.url("sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(
    Resolver.ivyStylePatterns)

resolvers += Classpaths.typesafeResolver

addSbtPlugin("com.typesafe.sbtscalariform" % "sbtscalariform" % "0.3.1")

addSbtPlugin("fi.jawsy.sbtplugins" %% "sbt-jrebel-plugin" % "0.9.0")

addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.0.10")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.1.1")

addSbtPlugin("com.typesafe.startscript" % "xsbt-start-script-plugin" % "0.5.1")

libraryDependencies <+= sbtVersion(v => "com.mojolly.scalate" %% "xsbt-scalate-generator" % (v + "-0.1.6"))

externalResolvers <<= resolvers map { Resolver.withDefaultResolvers(_, scalaTools = false) }