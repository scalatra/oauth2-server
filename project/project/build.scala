import sbt._
import Keys._

object PluginsBuild extends Build {

  val sbtGenPom = uri("git://github.com/geishatokyo/sbt-pom-gen-plugin.git#176f70039e3b592a4bd897090c1c8f32b0484322")
  val scct = uri("git://github.com/mtkopone/sbt-scct.git")
  val startScript = uri("git://github.com/casualjim/xsbt-start-script-plugin.git#59230926295fdf17f588bb37ab7fc2e9b98ffc11")
//  val sbtRequireJs = uri("git://github.com/scalatra/sbt-requirejs")
//  val sbtRequireJs = file("/Users/ivan/projects/sbt-requirejs")
  lazy val root = Project("plugins", file(".")).dependsOn(scct).dependsOn(startScript) //.dependsOn(sbtRequireJs) //settings (scalacOptions += "-deprecation")

//  lazy val scalateGenerate = ProjectRef(uri("git://github.com/mojolly/xsbt-scalate-generate.git"), "xsbt-scalate-generator")
}