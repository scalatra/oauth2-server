package io.backchat.oauth2

import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import akka.actor.ActorSystem
import org.eclipse.jetty.annotations.AnnotationConfiguration
import org.eclipse.jetty.servlet.{ DefaultServlet, ServletHolder }
import OAuth2Imports._
import org.eclipse.jetty.util.thread.ExecutorThreadPool
import java.util.concurrent.Executors

object JettyMain {

  def confKey(path: String) = "backchat.oauth2." + path

  def main(args: Array[String]) = {

    implicit val system = ActorSystem("oauth2server")

    val oauth = OAuth2Extension(system)

    val server: Server = new Server

    server setGracefulShutdown 5000
    server setSendServerVersion false
    server setSendDateHeader true
    server setStopAtShutdown true

    val connector = new SelectChannelConnector
    connector setHost oauth.web.host
    connector setPort oauth.web.port
    connector setName "Backchat OAuth2 Server"
    connector setMaxIdleTime 90.seconds.toMillis.toInt
    connector setSoLingerTime 0
    connector setReuseAddress true
    connector setThreadPool new ExecutorThreadPool(Executors.newCachedThreadPool())
    server addConnector connector

    val webApp = new WebAppContext

    webApp setContextPath "/"
    webApp.setConfigurations(Array(new AnnotationConfiguration))
    webApp setResourceBase oauth.web.public
    webApp addServlet (new ServletHolder(new HomeServlet), "/*")
    webApp addServlet (new ServletHolder(new ClientsApp), "/clients/*")
    webApp addServlet (new ServletHolder(new DefaultServlet()), "/img/*")
    webApp addServlet (new ServletHolder(new DefaultServlet()), "/js/*")
    webApp addServlet (new ServletHolder(new DefaultServlet()), "/css/*")
    webApp addServlet (new ServletHolder(new DefaultServlet()), "/")

    server setHandler webApp

    sys.addShutdownHook {
      server.stop()
      system.shutdown()
    }

    server.start()
  }
}