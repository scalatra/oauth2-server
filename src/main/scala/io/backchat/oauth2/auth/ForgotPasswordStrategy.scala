package io.backchat.oauth2
package auth

import org.scalatra.auth.ScentryStrategy
import org.scalatra.servlet.ServletBase
import scalaz._
import Scalaz._

class ForgotPasswordStrategy[UserClass <: AppUser[_]](protected val app: ServletBase, forgotPasswordProvider: ForgotPasswordProvider[UserClass]) extends ScentryStrategy[UserClass] {

  override val name = 'forgot_password

  override def afterAuthenticate(winningStrategy: Symbol, user: UserClass) {
    if (winningStrategy == 'user_password)
      forgotPasswordProvider.rememberedPassword(user, app.remoteAddress)
  }

  def authenticate() = none[UserClass]
}

