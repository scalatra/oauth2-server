package org.scalatra
package oauth2

import akka.actor.ActorSystem
import model.OAuth2Response
import OAuth2Imports._
import net.liftweb.json._

class PermissionsCrudApp(implicit protected val system: ActorSystem) extends OAuth2ServerBaseApp {

  before("/") {
    if (isAnonymous) scentry.authenticate("remember_me")
    if (isAnonymous && scentry.authenticate().isEmpty) unauthenticated()
  }

  def page = params.getOrElse("page", "1").toInt max 1
  def pageSize = params.getOrElse("pageSize", "20").toInt max 1

  get("/") {
    val clients = oauth.clients.find(MongoDBObject()).limit(pageSize).skip((page - 1) * pageSize)
    format match {
      case "json" ⇒ OAuth2Response(JArray(clients.toList.map(c ⇒ Extraction.decompose(c))))
      case _      ⇒ jade("clients", "clients" -> clients)
    }
  }
}
