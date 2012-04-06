package io.backchat.oauth2

import auth.{OAuth2ServerBaseApp, AuthenticationSupport}
import model._
import org.scalatra._
import org.scalatra.scalate.ScalateSupport
import OAuth2Imports._
import akka.actor.ActorSystem
import scalaz._
import Scalaz._
import org.scalatra.auth.Scentry
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import java.io.PrintWriter
import org.fusesource.scalate.servlet.ServletTemplateEngine
import javax.servlet.{ FilterConfig, ServletConfig }
import org.fusesource.scalate.{ Binding, TemplateEngine }

class HomeServlet(implicit protected val system: ActorSystem) extends OAuth2ServerBaseApp {

  get("/") {
    jade("hello-scalate")
  }

  notFound {
    serveStaticResource() getOrElse resourceNotFound()
  }

}
