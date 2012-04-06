package io.backchat.oauth2
package auth

import org.scalatra.servlet.ServletBase
import org.scalatra.auth.ScentryStrategy
import scalaz._
import Scalaz._

class PasswordStrategy[UserClass <: AppUser[_]](protected val app: ServletBase, userProvider: UserProvider[UserClass]) extends ScentryStrategy[UserClass] {

  override val name = 'user_password

  private def login = app.params.get("login") flatMap (_.blankOption)
  private def password = app.params.get("password") flatMap (_.blankOption)

  override def isValid = {
    login.isDefined && password.isDefined
  }

  /**
   * Authenticates a user by validating the username (or email) and password request params.
   */
  def authenticate: Option[UserClass] = {
    logger debug "Authenticating in UserPasswordStrategy with: %s, %s".format(~login, app.remoteAddress)
    val usr = userProvider.login(login.get, password.get, app.remoteAddress)
    usr.toOption
  }
}

