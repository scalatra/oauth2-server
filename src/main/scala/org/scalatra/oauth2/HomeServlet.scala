package org.scalatra
package oauth2

import model._
import akka.actor.ActorSystem

class HomeServlet(implicit protected val system: ActorSystem)
    extends OAuth2ServerBaseApp
    with AuthenticationApp[Account] {

  before("/") {
    if (isAnonymous) scentry.authenticate("remember_me")
  }
  before() {
    contentType = "text/html"
  }

  def requiresHttps = oauth.web.sslRequired && !this.isHttps

  before(requiresHttps) {
    halt(400, "The request needs to be secure")
  }

  get("/") {
    jade("angular")
  }

  get("/check_auth") {
    if (isAnonymous && scentry.authenticate().isEmpty) unauthenticated()
  }
}
