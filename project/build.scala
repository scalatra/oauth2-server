import sbt._
import Keys._

object OAuth2ServerBuild extends Build {
  val dispatchLiftJson =
         uri("git://github.com/backchatio/dispatch-lift-json")

  val scalatraContrib = ProjectRef(file("/Users/ivan/projects/scalatra/scalatra-contrib"), "contrib-validation")
  val root = Project("oauth2-server", file(".")) dependsOn(dispatchLiftJson) dependsOn(scalatraContrib)
}

