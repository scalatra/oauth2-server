package io.backchat.oauth2
package auth

import org.scalatra.ScalatraBase
import scentry.{ ScentryStrategy, ScentrySupport }
import org.scalatra.servlet.ServletBase
import OAuth2Imports._
import model.{ fieldNames, Client, ClientDao }

/**
 * Provides a hook for the basic auth strategy
 *
 * for more details on usage check:
 * https://gist.github.com/732347
 */
trait BasicAuthSupport[UserType <: AppUser[_]] { self: ScalatraBase with ScentrySupport[UserType] ⇒

  def realm: String

  protected def basicAuth() = {

    if (!request.providesAuth) {
      response.headers("WWW-Authenticate") = "Basic realm=\"%s\"" format realm
      halt(401, "Unauthenticated")
    }
    if (!request.isBasicAuth) {
      halt(400, "Bad Request")
    }
    scentry.authenticate("Basic")
  }

}

abstract class BasicAuthStrategy[UserType <: AnyRef](protected val app: ScalatraBase, realm: String, override val name: String)
    extends ScentryStrategy[UserType] {

  private val REMOTE_USER = "REMOTE_USER"
  protected def challenge = "Basic realm=\"%s\"" format realm

  override def isValid = {
    val v = app.request.isBasicAuth && app.request.providesAuth
    println("This request provides basic auth? " + v.toString)
    v
  }

  override def beforeAuthenticate { println("before authenticate " + getClass.getName) }

  override def afterAuthenticate(winningStrategy: String, user: UserType) {
    println("after authenticate " + getClass.getName)
  }

  override def beforeSetUser(user: UserType) {
    println("before set user " + getClass.getName)
  }

  override def beforeFetch[IdType](userId: IdType) {
    println("before fetch user " + getClass.getName)
  }

  override def afterFetch(user: UserType) {
    println("before fetch user " + getClass.getName)
  }

  override def beforeLogout(user: UserType) {
    println("before logout " + getClass.getName)
  }

  def authenticate() = {
    val req = app.request
    validate(req.username, req.password)
  }

  protected def getUserId(user: UserType): String
  protected def validate(userName: String, password: String): Option[UserType]

  override def afterSetUser(user: UserType) {
    println("after set user " + getClass.getName)
    app.response.headers(REMOTE_USER) = getUserId(user)
  }

  override def unauthenticated() {
    println("unauthenticated " + getClass.getName)
    app.response.headers("WWW-Authenticate") = challenge
    app.halt(401, "Unauthenticated")
  }

  override def afterLogout(user: UserType) {
    println("after logout " + getClass.getName)
    app.response.headers(REMOTE_USER) = ""
  }
}

class AppUserBasicAuth[UserClass <: AppUser[_]](app: ScalatraBase, realm: String, userProvider: UserProvider[UserClass])
    extends BasicAuthStrategy[UserClass](app, realm, "resource_owner_basic") {

  protected def getUserId(user: UserClass) = user.idString

  protected def validate(userName: String, password: String) =
    userProvider.login(userName, password, app.remoteAddress).toOption
}

class ClientBasicAuth(app: ScalatraBase, realm: String, clientsDao: ClientDao)
    extends BasicAuthStrategy[Client](app, realm, "client_basic") {
  protected def getUserId(user: Client) = user.id.toString

  protected def validate(userName: String, password: String) =
    clientsDao.findOne(Map(fieldNames._id -> userName, fieldNames.secret -> password))
}
