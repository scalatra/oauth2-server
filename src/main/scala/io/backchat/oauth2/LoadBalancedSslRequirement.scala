package io.backchat.oauth2

import akka.actor.ActorSystem
import java.net.URI
import org.eclipse.jetty.http.HttpHeaders
import org.scalatra.{ Initializable, ResponseStatus, ScalatraBase, Handler }

trait LoadBalancedSslRequirement extends Handler with LoadBalancerPing { self: ScalatraBase ⇒

  implicit protected def system: ActorSystem

  abstract override def handle(req: this.RequestT, res: this.ResponseT) {
    if (OAuth2Extension(system).web.sslRequired && !this.isHttps && !req.pathInfo.contains("eb_ping")) {
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