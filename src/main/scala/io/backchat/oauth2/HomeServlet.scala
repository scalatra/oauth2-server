package io.backchat.oauth2

import model._
import akka.actor.ActorSystem

class HomeServlet(implicit protected val system: ActorSystem)
    extends OAuth2ServerBaseApp
    with AuthenticationApp[ResourceOwner] {

  before() {
    contentType = "text/html"
  }

  get("/") {
    jade("hello-scalate")
  }

  notFound {
    serveStaticResource() getOrElse resourceNotFound()
  }

}
