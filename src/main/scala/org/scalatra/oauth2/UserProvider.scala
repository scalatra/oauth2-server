package org.scalatra
package oauth2

import org.scalatra.validation.ValidationError
import commands._
import scalaz._
import Scalaz._
import akka.actor.ActorSystem

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

trait AppAuthSession[UserClass <: AppUser[_]] {
  def idString: String
  def userIdString: String
  def token: AppToken

  def account(implicit system: ActorSystem): UserClass
}

trait UserProvider[UserClass <: AppUser[_]] {

  def login(loginCommand: LoginCommand): ValidationNEL[ValidationError, UserClass]
  def findUserById(id: String): Option[UserClass]
  def findByLoginOrEmail(loginOrEmail: String): Option[UserClass]
  def register(registerCommand: RegisterCommand): ValidationNEL[ValidationError, UserClass]
  def confirm(activateCommand: ActivateAccountCommand): ValidationNEL[ValidationError, UserClass]

}

trait AuthSessionProvider[AuthSessionClass <: AppAuthSession[_ <: AppUser[_]]]

trait RememberMeProvider[UserClass <: AppUser[_]] {
  def loginFromRemember(token: String): ValidationNEL[ValidationError, UserClass]
  def remember(owner: UserClass): Validation[ValidationError, String]
}

trait ForgotPasswordProvider[UserClass <: AppUser[_]] {
  def forgot(forgotCommand: ForgotCommand): ValidationNEL[ValidationError, UserClass]
  def resetPassword(resetCommand: ResetCommand): ValidationNEL[ValidationError, UserClass]
}

trait AuthenticatedChangePasswordProvider[UserClass <: AppUser[_]] {
  def changePassword(command: ChangePasswordCommand): ValidationNEL[ValidationError, UserClass]
}
