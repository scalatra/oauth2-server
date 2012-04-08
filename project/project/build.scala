import sbt._
import Keys._

object PluginsBuild extends Build {
  lazy val root = Project("plugins", file(".")) dependsOn (scalateGenerate) settings (scalacOptions += "-deprecation")
  lazy val scalateGenerate = ProjectRef(uri("git://github.com/mojolly/xsbt-scalate-generate.git"), "xsbt-scalate-generator")
}