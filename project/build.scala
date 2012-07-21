import sbt._
import Keys._

object OAuth2ServerBuild extends Build {
  val dispatchLiftJson =
         uri("git://github.com/backchatio/dispatch-lift-json")

  val contribLocation = file("/Users/ivan/projects/scalatra/scalatra-contrib")
  def contrib(name: String) = ProjectRef(contribLocation, "contrib-" + name)

  val root = (Project("oauth2-server", file("."))
                dependsOn(dispatchLiftJson)
                dependsOn(contrib("commons"))
                dependsOn(contrib("validation")))
}

