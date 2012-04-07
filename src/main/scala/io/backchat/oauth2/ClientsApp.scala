package io.backchat.oauth2

import akka.actor.ActorSystem
import model.Enums.AuthorizationType
import model.{ Token, Client }
import scalaz._
import Scalaz._
import OAuth2Imports._

class ClientsApp(implicit protected val system: ActorSystem) extends OAuth2ServerBaseApp {

  before(isAnonymous) { unauthenticated() }

  def page = params.getOrElse("page", "1").toInt max 1
  def pageSize = params.getOrElse("pageSize", "20").toInt max 1

  get("/") {
    jade("clients", "clients" -> oauth.clients.find(MongoDBObject()).limit(pageSize).skip((page - 1) * pageSize))
  }

  post("/") {
    val client = Client(
      Token.generate.token,
      ~params.get("profile"),
      ~params.get("display_name"),
      AuthorizationType.withName(~params.get("auth_type")),
      scope = multiParams("scope").toList,
      redirectUri = params.get("redirect_uri"),
      link = params.get("link"))

    val validated = oauth.clients.validateClient(client)
    validated foreach oauth.clients.save
    validated.fold(
      errs ⇒ jade("client/new", templateData("errors" -> errs.list, "client" -> client): _*),
      client ⇒ {
        flash("success") = "Created client %s".format(client.displayName)
        redirect("/clients")
      })
  }

  get("/:id") {
    oauth.clients.findOneByID(new ObjectId(params("id"))) map { client ⇒
      jade("clients/edit", templateData("client" -> client): _*)
    } getOrElse halt(404, "No client exists")
  }

  get("/new") {
    jade("clients/new", templateData(): _*)
  }

  post("/:id") {
    (oauth.clients.findOneByID(new ObjectId(params("id"))) map { client ⇒
      val update = client.copy(
        profile = ~params.get("profile"),
        displayName = ~params.get("display_name"),
        authorizationType = AuthorizationType.withName(~params.get("auth_type")),
        scope = multiParams("scope").toList,
        redirectUri = params.get("redirect_uri"),
        link = params.get("link"))
      val saved = oauth.clients.validateClient(update)
      saved foreach oauth.clients.save
      saved.fold(
        errs ⇒ jade("edit", templateData("errors" -> errs.list, "client" -> client): _*),
        cl ⇒ {
          flash("success") = "Updated client %s".format(cl.displayName)
          redirect("/clients")
        })
    }) getOrElse halt(404, "Client not found")
  }

  delete("/:id") {
    oauth.clients.removeById(new ObjectId(params("id")))
    flash("success") = "Client deleted"
    redirect("/clients")
  }

  private val authTypes = AuthorizationType.values.map(v ⇒ (v.toString, v.toString.humanize)).toSeq
  private val profiles = List(
    "Web Application" -> "Web application",
    "User-Agent" -> "Browser based application",
    "Native Application" -> "Desktop, mobile application")

  private val lists = Map("authTypes" -> authTypes, "profiles" -> profiles)

  def templateData(pairs: (String, Any)*) = (lists ++ Map(pairs: _*)).toSeq

}
