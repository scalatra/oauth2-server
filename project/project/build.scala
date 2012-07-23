import sbt._
import Keys._

object PluginsBuild extends Build {

//  val sbtRequireJs = uri("git://github.com/scalatra/sbt-requirejs")
//  val sbtRequireJs = file("/Users/ivan/projects/sbt-requirejs")
  lazy val root = Project("plugins", file(".")) //.dependsOn(sbtRequireJs) //settings (scalacOptions += "-deprecation")

//  lazy val scalateGenerate = ProjectRef(uri("git://github.com/mojolly/xsbt-scalate-generate.git"), "xsbt-scalate-generator")
}