import sbt._
import Keys._

object OAuth2ServerBuild extends Build {
  val dispatchLiftJson =
         uri("git://github.com/backchatio/dispatch-lift-json")

  val contribLocation = file("/Users/ivan/projects/scalatra/scalatra-contrib")
  def contrib(name: String) = ProjectRef(contribLocation, "contrib-" + name)

  val scalatraLocation = file("/Users/ivan/projects/scalatra/scalatra")
  def scalatra(name: String) =
    ProjectRef(scalatraLocation, if (name == null || name.trim.isEmpty) "scalatra" else "scalatra-" + name)

  val root = (Project("oauth2-server", file("."))
                dependsOn(dispatchLiftJson) //)
                dependsOn(scalatra(""))
                dependsOn(scalatra("auth"))
                dependsOn(scalatra("lift-json"))
                dependsOn(scalatra("swagger"))
                dependsOn(scalatra("slf4j"))
                dependsOn(scalatra("scalate"))
                dependsOn(scalatra("specs2") % "test->compile")
                dependsOn(contrib("commons"))
                dependsOn(contrib("validation")))
}

