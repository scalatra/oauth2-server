package org.scalatra
package oauth2
package auth

import org.scalatra.ScalatraBase
import liftjson.LiftJsonSupport
import commands.{ OAuth2Command, LoginCommand }
import service.CommandHandler
import org.scalatra.auth.ScentryStrategy
import scalaz._
import Scalaz._
import OAuth2Imports._

class PasswordStrategy[UserClass <: AppAuthSession](
    protected val app: ScalatraBase with LiftJsonSupport,
    command: LoginCommand,
    handler: CommandHandler) extends ScentryStrategy[UserClass] {

  override val name = "user_password"

  override def isValid = command.isValid

  /**
   * Authenticates a user by validating the username (or email) and password request params.
   */
  def authenticate: Option[UserClass] = handler.execute(command).toOption.map(_.asInstanceOf[UserClass])

  /*
  override def beforeSetUser(user: UserClass) {
    println("before set user " + getClass.getName)
  }

  override def afterAuthenticate(winningStrategy: String, user: UserClass) {
    if (winningStrategy == "user_password") app.user = userProvider.loggedIn(user, app.remoteAddress)
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
*/

}

