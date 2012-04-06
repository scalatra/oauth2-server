package io.backchat.oauth2

import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.servlet.ServletHolder
import akka.actor.ActorSystem

object JettyMain {

  def confKey(path: String) = "backchat.oauth2." + path

  def main(args: Array[String]) = {

    implicit val system = ActorSystem("oauth2server")

    val server: Server = new Server

    server setGracefulShutdown 5000
    server setSendServerVersion false
    server setSendDateHeader true
    server setStopAtShutdown true

    val connector = new SelectChannelConnector
    connector setPort 8080
    connector setMaxIdleTime 90000
    server addConnector connector

    val webapp = system.settings.config.getString(confKey("web.public"))
    val webApp = new WebAppContext
    webApp setContextPath "/"
    webApp setResourceBase webapp
    webApp setDescriptor (webapp + "/WEB-INF/web.xml")
    webApp addServlet (new ServletHolder(new HomeServlet), "/*")

    server setHandler webApp

    sys.addShutdownHook {
      server.stop()
      system.shutdown()
    }

    server.start()
  }
}