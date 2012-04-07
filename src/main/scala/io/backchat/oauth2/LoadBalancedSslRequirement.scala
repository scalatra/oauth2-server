package io.backchat.oauth2

import akka.actor.ActorSystem
import OAuth2Imports._
import java.net.URI
import org.eclipse.jetty.http.HttpHeaders
import org.scalatra.{ ResponseStatus, ScalatraBase }
import org.scalatra.servlet.ServletBase

trait LoadBalancedSslRequirement extends ServletBase {

  implicit def system: ActorSystem

  abstract override def handle(req: RequestT, res: ResponseT) {
    if (OAuth2Extension(system).web.sslRequired && !this.isHttps && !req.pathInfo.contains("eb_ping")) {
      val oldUri = req.uri
      val url = new URI("https", oldUri.getAuthority, oldUri.getPath, oldUri.getQuery, oldUri.getFragment).toASCIIString
      res.status = ResponseStatus(301)
      res.addHeader(HttpHeaders.LOCATION, url)
    } else {
      super.handle(req, res)
    }
  }

}

trait LoadBalancerPing { self: ScalatraBase ⇒

  get("/eb_ping") {
    "pong"
  }

}