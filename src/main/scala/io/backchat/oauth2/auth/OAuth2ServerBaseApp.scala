package io.backchat.oauth2
package auth

import model.ResourceOwner
import org.scalatra.{ CookieSupport, FlashMapSupport, ScalatraServlet }
import org.scalatra.scalate.ScalateSupport
import akka.actor.ActorSystem
import java.net.URI
import org.eclipse.jetty.http.HttpHeaders

trait LoadBalancedSslRequirement extends org.scalatra.servlet.ServletBase {

  implicit def system: ActorSystem

  abstract override def handle(req: RequestT, res: ResponseT) {
    if (OAuth2Extension(system).web.sslRequired && !this.isHttps && !req.getRequestURI.contains("eb_ping")) {
      val oldUri = new URI(req.getRequestURL.toString)
      val url = new URI("https", oldUri.getAuthority, oldUri.getPath, oldUri.getQuery, oldUri.getFragment).toASCIIString
      res.setStatus(301)
      res.addHeader(HttpHeaders.LOCATION, res.encodeRedirectURL(url))
    } else {
      super.handle(req, res)
    }
  }

  get("/eb_ping") { "pong" }

}

trait OAuth2ServerBaseApp extends ScalatraServlet
    with FlashMapSupport
    with CookieSupport
    with ScalateSupport
    with AuthenticationSupport[ResourceOwner] {

  implicit protected def system: ActorSystem
  val oauth = OAuth2Extension(system)

  protected val userManifest = manifest[ResourceOwner]

  protected lazy val authProvider = oauth.userProvider

  /**
   * Builds a full URL from the given relative path. Takes into account the port configuration, https, ...
   *
   * @param path a relative path
   *
   * @return the full URL
   */
  protected def buildFullUrl(path: String) = {
    if (path.startsWith("http")) path else {
      "http%s://%s%s/%s".format(
        if (oauth.web.sslRequired || this.isHttps) "s" else "",
        oauth.web.domainWithPort,
        request.getContextPath,
        path)
    }
  }
}