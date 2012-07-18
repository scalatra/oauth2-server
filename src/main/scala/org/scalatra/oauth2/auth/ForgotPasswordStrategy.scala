package org.scalatra
package oauth2
package auth

import org.scalatra.auth.ScentryStrategy
import org.scalatra.ScalatraBase
import scalaz._
import Scalaz._

class ForgotPasswordStrategy[UserClass <: AppUser[_]](protected val app: ScalatraBase, forgotPasswordProvider: ForgotPasswordProvider[UserClass]) extends ScentryStrategy[UserClass] {

  override val name = "forgot_password"

  override def isValid = !app.request.providesAuth

  override def afterAuthenticate(winningStrategy: String, user: UserClass) {
    println("after authtenticate " + getClass.getName)
    if (winningStrategy == "user_password")
      forgotPasswordProvider.rememberedPassword(user, app.remoteAddress)
  }

  def authenticate() = none[UserClass]

  override def beforeAuthenticate { println("before authenticate " + getClass.getName) }

  override def beforeSetUser(user: UserClass) {
    println("before set user " + getClass.getName)
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

