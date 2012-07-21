package org.scalatra
package oauth2

import command.FieldError
import scalaz._
import Scalaz._

trait AppUser[TPassword] {
  def idString: String
  def login: String
  def name: String
  def email: String
  def password: TPassword
  def remembered: AppToken
  def confirmation: AppToken
  def reset: AppToken

  def isConfirmed: Boolean
  def isReset: Boolean
}

trait AppToken {
  def token: String
}

trait UserProvider[UserClass <: AppUser[_]] {

  def login(loginOrEmail: String, password: String, ipAddress: String = ""): Validation[FieldError, UserClass]
  def findUserById(id: String): Option[UserClass]
  def loggedIn(user: UserClass, ipAddress: String): UserClass
  def findByLoginOrEmail(loginOrEmail: String): Option[UserClass]
  def register(
    login: Option[String],
    email: Option[String],
    name: Option[String],
    password: Option[String],
    passwordConfirmation: Option[String]): ValidationNEL[FieldError, UserClass]
  def confirm(token: String): Validation[FieldError, UserClass]
  def validate(user: UserClass): ValidationNEL[FieldError, UserClass]
}

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
