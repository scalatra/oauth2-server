package org.scalatra
package oauth2
package auth

import org.scalatra.{ CookieSupport, CookieOptions, Cookie }
import org.scalatra.ScalatraBase
import scalaz._
import org.scalatra.auth.ScentryStrategy
import OAuth2Imports._
import commands.OAuth2Command
import service.{ AuthenticationService, CommandHandler }
import akka.actor.ActorSystem

object RememberMeStrategy {
  val CookieKey = "scalatra.oauth2.remember"
}

/**
 * Authentication strategy to authenticate a user from a cookie.
 */
class RememberMeStrategy[UserClass <: AppAuthSession[_ <: AppUser[_]]](
  protected val app: ScalatraBase with CookieSupport,
  service: AuthenticationService,
  cookieKey: String = RememberMeStrategy.CookieKey)(implicit system: ActorSystem)
    extends ScentryStrategy[UserClass] {

  override val name = "remember_me"

  private val oneWeek = 7 * 24 * 3600

  override def isValid = {
    app.cookies.get(cookieKey).flatMap(_.blankOption).isDefined
  }

  /**
   * After authentication, sets the remember-me cookie on the response.
   */
  override def afterAuthenticate(winningStrategy: String, user: UserClass) = {
    if (winningStrategy == "remember_me" ||
      (winningStrategy == "user_password" && app.params.getOrElse("remember", "").asCheckboxBool)) {
      logger debug "Remembering user [%s]".format(user.account.email)
      service.remember(user) foreach { token ⇒
        logger info "Setting cookie for user [%s]".format(user.account.email)
        app.cookies.update(cookieKey, token)(CookieOptions(maxAge = oneWeek, httpOnly = true))
      }
    }
  }

  /**
   * Authenticates a user by validating the remember-me cookie.
   */
  def authenticate = {
    app.cookies.get(cookieKey) flatMap { token ⇒
      service.loginFromRemember(token).toOption.map(_.asInstanceOf[UserClass])
    }
  }

  /**
   * Clears the remember-me cookie for the specified user.
   */
  override def beforeLogout(user: UserClass) = {
    logger debug "Entering before logout in remember me strategy"
    if (user != null) {
      service.remember(user)
      logger debug "Removed cookie for user [%s]".format(user.account.login)
    }
    app.cookies.get(cookieKey) foreach { _ ⇒ app.cookies.update(cookieKey, null) }

  }
  /*
  override def beforeSetUser(user: UserClass) {
    println("before set user " + getClass.getName)
  }

  override def beforeFetch[IdType](userId: IdType) {
    println("before fetch user " + getClass.getName)
  }

  override def afterFetch(user: UserClass) {
    println("before fetch user " + getClass.getName)
  }

  override def afterSetUser(user: UserClass) {
    println("after set user " + getClass.getName)
  }

  override def unauthenticated() {
    println("unauthenticated " + getClass.getName)
  }

  override def afterLogout(user: UserClass) {
    println("after logout " + getClass.getName)
  }*/

}

