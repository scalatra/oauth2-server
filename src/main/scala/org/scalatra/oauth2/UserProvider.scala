package org.scalatra
package oauth2

import command.FieldError
import commands.{ActivateAccountCommand, RegisterCommand, LoginCommand}
import scalaz._
import Scalaz._

trait AppUser[TPassword] {
  def idString: String
  def login: String
  def name: String
  def email: String
  def password: TPassword
  def confirmation: AppToken
  def reset: AppToken

  def isConfirmed: Boolean
  def isReset: Boolean
}

trait AppToken {
  def token: String
}

trait AppAuthSession {
  def idString: String
  def userId: String
  def token: AppToken
}

trait UserProvider[UserClass <: AppUser[_]] {

  def login(loginCommand: LoginCommand): ValidationNEL[FieldError, UserClass]
  def findUserById(id: String): Option[UserClass]
  def findByLoginOrEmail(loginOrEmail: String): Option[UserClass]
  def register(registerCommand: RegisterCommand): ValidationNEL[FieldError, UserClass]
  def confirm(activateCommand: ActivateAccountCommand): ValidationNEL[FieldError, UserClass]

}

trait AuthSessionProvider[AuthSessionClass <: AppAuthSession]

trait RememberMeProvider[UserClass <: AppUser[_]] {
  def loginFromRemember(token: String): Validation[FieldError, UserClass]
  def remember(owner: UserClass): Validation[FieldError, String]
}

trait ForgotPasswordProvider[UserClass <: AppUser[_]] {
  def forgot(loginOrEmail: Option[String]): Validation[FieldError, UserClass]
  def resetPassword(token: String, password: String, passwordConfirmation: String): ValidationNEL[FieldError, UserClass]
  def rememberedPassword(owner: UserClass, ipAddress: String): UserClass
}

trait AuthenticatedChangePasswordProvider[UserClass <: AppUser[_]] {
  def changePassword(owner: UserClass, oldPassword: String, password: String, passwordConfirmation: String): Validation[FieldError, UserClass]
}

trait OAuthUserProvider
