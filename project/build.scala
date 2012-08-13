import sbt._
import Keys._

object OAuth2ServerBuild extends Build {

  // val sbtGenPom = uri("git://github.com/geishatokyo/sbt-pom-gen-plugin.git#176f70039e3b592a4bd897090c1c8f32b0484322")

//  val dispatchLiftJson =
//         uri("git://github.com/backchatio/dispatch-lift-json")

//  val contribLocation = uri("git://github.com/scalatra/scalatra-contrib#51c7268bab8b8c5bbd205f72e8d333b41bfdf924")
  val contribLocation = file("/Users/ivan/projects/scalatra/scalatra-contrib")
  def contrib(name: String) = ProjectRef(contribLocation, "contrib-" + name)

//  val scalatraLocation = uri("git://github.com/scalatra/scalatra#9a7ab2c2cf1134d2c3ad03206d6a82efc81f6ec9")
  val scalatraLocation = file("/Users/ivan/projects/scalatra/scalatra")
  def scalatra(name: String = null) =
    ProjectRef(scalatraLocation, if (name == null || name.trim.isEmpty) "scalatra" else "scalatra-" + name)

  val root = (Project("oauth2-server", file(".")))
//                dependsOn(scalatra(""))
//                dependsOn(scalatra("auth"))
//                dependsOn(scalatra("lift-json"))
//                dependsOn(scalatra("swagger"))
//                dependsOn(scalatra("slf4j"))
//                dependsOn(scalatra("scalate"))
//                dependsOn(scalatra("specs2") % "test->compile")
//                dependsOn(contrib("validation")))
}

