package org.scalatra
package oauth2

import model._
import akka.actor.ActorSystem
import net.liftweb.json.Extraction

class HomeServlet(implicit protected val system: ActorSystem)
    extends OAuth2ServerBaseApp
    with AuthenticationApp[AuthSession] {

  if (!oauth.isTest) {
    val guarded = Seq("", "login", "register", "forgot", "reset")
    guarded foreach (s ⇒ xsrfGuard("/" + s))
  }

  before("/") {
    if (isAnonymous) scentry.authenticate("remember_me")
  }
  before() {
    if (format != "json" || format != "xml")
      contentType = "text/html"
  }

  def requiresHttps = oauth.web.sslRequired && !this.isHttps

  before(requiresHttps) {
    halt(400, "The request needs to be secure")
  }

  get("/") {
    format match {
      case "json" | "xml" if isAuthenticated ⇒ OAuth2Response(Extraction.decompose(user.account))
      case _                                 ⇒ jade("angular")
    }
  }

}
