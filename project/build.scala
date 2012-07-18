import sbt._
import Keys._

object OAuth2ServerBuild extends Build {
  val dispatchLiftJson =
         uri("git://github.com/backchatio/dispatch-lift-json")
  val root = Project("oauth2-server", file(".")) dependsOn(dispatchLiftJson)
}

