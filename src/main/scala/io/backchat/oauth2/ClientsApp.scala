package io.backchat.oauth2

import akka.actor.ActorSystem

class ClientsApp(implicit protected val system: ActorSystem) extends OAuth2ServerBaseApp {

  get("/") {

  }
}
