package io.backchat.oauth2
package auth

import org.scalatra.ScalatraBase
import org.scalatra.auth.{ ScentryStrategy, ScentrySupport }
import org.scalatra.servlet.ServletBase
import OAuth2Imports._
import model.{ fieldNames, Client, ClientDao, ResourceOwner }

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
    scentry.authenticate('Basic)
  }

}

abstract class BasicAuthStrategy[UserType <: AnyRef](protected val app: ServletBase, realm: String, override val name: Symbol)
    extends ScentryStrategy[UserType] {

  private val REMOTE_USER = "REMOTE_USER".intern
  protected def challenge = "Basic realm=\"%s\"" format realm

  override def isValid = {
    app.request.isBasicAuth && app.request.providesAuth
  }

  def authenticate() = {
    val req = app.request
    validate(req.username, req.password)
  }

  protected def getUserId(user: UserType): String
  protected def validate(userName: String, password: String): Option[UserType]

  override def afterSetUser(user: UserType) {
    app.response.headers(REMOTE_USER) = getUserId(user)
  }

  override def unauthenticated() {
    app.response.headers("WWW-Authenticate") = challenge
    app.halt(401, "Unauthenticated")
  }

  override def afterLogout(user: UserType) {
    app.response.headers(REMOTE_USER) = ""
  }
}

class AppUserBasicAuth[UserClass <: AppUser[_]](app: ServletBase, realm: String, userProvider: UserProvider[UserClass])
    extends BasicAuthStrategy[UserClass](app, realm, 'resource_owner_basic) {

  protected def getUserId(user: UserClass) = user.idString

  protected def validate(userName: String, password: String) =
    userProvider.login(userName, password, app.remoteAddress).toOption
}

class ClientBasicAuth(app: ServletBase, realm: String, clientsDao: ClientDao)
    extends BasicAuthStrategy[Client](app, realm, 'client_basic) {
  protected def getUserId(user: Client) = user.id.toString

  protected def validate(userName: String, password: String) =
    clientsDao.findOne(Map(fieldNames._id -> userName, fieldNames.secret -> password))
}
