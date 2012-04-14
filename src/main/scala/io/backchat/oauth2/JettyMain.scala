package io.backchat.oauth2

import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import akka.actor.ActorSystem
import OAuth2Imports._
import org.eclipse.jetty.util.thread.ExecutorThreadPool
import java.util.concurrent.Executors
import org.eclipse.jetty.annotations.{ MultiPartConfigAnnotationHandler, AnnotationConfiguration }
import ro.isdc.wro.http.WroFilter
import collection.JavaConverters._
import org.eclipse.jetty.servlet.{ FilterMapping, FilterHolder, DefaultServlet, ServletHolder }
import org.eclipse.jetty.plus.servlet.ServletHandler
import ro.isdc.wro.model.resource.processor.impl.ExtensionsAwareProcessorDecorator
import ro.isdc.wro.extensions.processor.js.CoffeeScriptProcessor

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

    val ac = new AnnotationConfiguration()
    webApp setResourceBase oauth.web.public
    //    webApp setDescriptor (oauth.web.public + "/WEB-INF/web.xml")
    webApp setContextPath "/"
    webApp setParentLoaderPriority true

    ExtensionsAwareProcessorDecorator.decorate(new CoffeeScriptProcessor()).addExtension("coffee");
    val wro4jFilter = new FilterHolder(classOf[WroFilter])
    wro4jFilter.setName("WebResourceOptimizer")
    wro4jFilter.setInitParameters(Map(
      "managerFactoryClassName" -> "io.backchat.oauth2.OAuthWroManagerFactory").asJava)
    //      "postProcessors" -> "cssVariables.css,cssMinJawr.css,googleClosureAdvanced.js").asJava)
    val mapping = new FilterMapping()
    mapping.setFilterName(wro4jFilter.getName)
    mapping.setPathSpec("/wro/*")
    val servletHandler = new ServletHandler
    servletHandler.addFilter(wro4jFilter, mapping)
    webApp.setServletHandler(servletHandler)

    webApp addServlet (new ServletHolder(new HomeServlet), "/*")
    webApp addServlet (new ServletHolder(new ClientsCrudApp), "/clients/*")
    webApp addServlet (new ServletHolder(new OAuthAuthentication), "/auth/*")
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