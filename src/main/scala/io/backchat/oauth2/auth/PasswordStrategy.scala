package io.backchat.oauth2
package auth

import org.scalatra.auth.ScentryStrategy
import scalaz._
import Scalaz._
import OAuth2Imports._
import org.scalatra.ScalatraBase

class PasswordStrategy[UserClass <: AppUser[_]](protected val app: ScalatraBase, userProvider: UserProvider[UserClass]) extends ScentryStrategy[UserClass] {

  override val name = "user_password"

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

  override def beforeSetUser(user: UserClass) {
    println("before set user " + getClass.getName)
  }

  override def afterAuthenticate(winningStrategy: String, user: UserClass) {
    println("after authenticate " + getClass.getName)
  }

  override def beforeFetch[IdType](userId: IdType) {
    println("before fetch user " + getClass.getName)
  }

  override def afterFetch(user: UserClass) {
    println("before fetch user " + getClass.getName)
  }

  override def beforeLogout(user: UserClass) {
    println("before logout " + getClass.getName)
  }

  override def afterSetUser(user: UserClass) {
    println("after set user " + getClass.getName)
  }

  override def unauthenticated() {
    println("unauthenticated " + getClass.getName)
  }

  override def afterLogout(user: UserClass) {
    println("after logout " + getClass.getName)
  }

}

