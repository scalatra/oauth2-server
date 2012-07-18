//package org.scalatra
package oauth2
//
//import org.eclipse.jetty.server.nio.SelectChannelConnector
//import org.eclipse.jetty.server.Server
//import org.eclipse.jetty.webapp.WebAppContext
//import akka.actor.ActorSystem
//import OAuth2Imports._
//import org.eclipse.jetty.util.thread.ExecutorThreadPool
//import java.util.concurrent.Executors
//import ro.isdc.wro.http.WroFilter
//import collection.JavaConverters._
//import ro.isdc.wro.model.resource.processor.impl.ExtensionsAwareProcessorDecorator
//import ro.isdc.wro.extensions.processor.js.CoffeeScriptProcessor
//import org.eclipse.jetty.servlet._
//import com.typesafe.config.ConfigFactory
//
//object JettyMain {
//
//
//
//  def main(args: Array[String]) = {
//
//    implicit val system = ActorSystem(org.scalatra.oauth2.ActorSystemName)
//    val oauth = OAuth2Extension(system)
//
//    val server: Server = new Server
//    server setGracefulShutdown 5000
//    server setSendServerVersion false
//    server setSendDateHeader true
//    server setStopAtShutdown true
//
//    val connector = new SelectChannelConnector
//    connector setHost oauth.web.host
//    connector setPort oauth.web.port
//    connector setName "Backchat OAuth2 Server"
//    connector setMaxIdleTime 30.seconds.toMillis.toInt
//    connector setSoLingerTime 0
//    connector setReuseAddress true
//    server addConnector connector
//
//    val webApp = new WebAppContext
//    webApp.getServletContext.setAttribute(org.scalatra.oauth2.ActorSystemContextKey, system)
//    webApp setContextPath "/"
//    webApp setResourceBase oauth.web.public
//    webApp setDescriptor (oauth.web.public + "/WEB-INF/web.xml")
//    webApp setContextPath "/"
//    webApp setParentLoaderPriority true
//    server setHandler webApp
//
//    sys.addShutdownHook {
//      server.stop()
//    }
//
//    server.start()
//  }
//}