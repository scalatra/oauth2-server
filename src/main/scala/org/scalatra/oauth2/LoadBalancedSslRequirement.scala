package org.scalatra
package oauth2

import akka.actor.ActorSystem
import java.net.URI
import org.eclipse.jetty.http.HttpHeaders
import org.scalatra.{ Initializable, ResponseStatus, ScalatraBase, Handler }
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }

trait LoadBalancedSslRequirement extends Handler with LoadBalancerPing { self: ScalatraBase ⇒

  implicit protected def system: ActorSystem

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    if (OAuth2Extension(system).web.sslRequired && !self.isHttps && !req.pathInfo.contains("eb_ping")) {
      val oldUri = req.uri
      val url = new URI("https", oldUri.getAuthority, oldUri.getPath, oldUri.getQuery, oldUri.getFragment).toASCIIString
      res.status = ResponseStatus(301)
      res.headers(HttpHeaders.LOCATION) = url
      res.outputStream.close()
    } else {
      super.handle(req, res)
    }
  }

}

trait LoadBalancerPing extends Initializable { self: ScalatraBase ⇒

  private[this] val defaultPingPath = "/eb_ping"
  def pingPath: String = defaultPingPath

  abstract override def initialize(config: ConfigT) {
    super.initialize(config)
    get(pingPath) { "pong" }
  }
}