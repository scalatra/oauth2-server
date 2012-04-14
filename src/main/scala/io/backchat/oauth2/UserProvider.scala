package io.backchat.oauth2

import model.Error
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

  def login(loginOrEmail: String, password: String, ipAddress: String = ""): Validation[Error, UserClass]
  def findUserById(id: String): Option[UserClass]
  def loggedIn(user: UserClass, ipAddress: String): UserClass
  def findByLoginOrEmail(loginOrEmail: String): Option[UserClass]
  def register(
    login: Option[String],
    email: Option[String],
    name: Option[String],
    password: Option[String],
    passwordConfirmation: Option[String]): ValidationNEL[Error, UserClass]
  def confirm(token: String): Validation[Error, UserClass]
}

trait RememberMeProvider[UserClass <: AppUser[_]] {
  def loginFromRemember(token: String): Validation[Error, UserClass]
  def remember(owner: UserClass): Validation[Error, String]
}

trait ForgotPasswordProvider[UserClass <: AppUser[_]] {
  def forgot(loginOrEmail: Option[String]): Validation[Error, UserClass]
  def resetPassword(token: String, password: String, passwordConfirmation: String): ValidationNEL[Error, UserClass]
  def rememberedPassword(owner: UserClass, ipAddress: String): UserClass
}

trait AuthenticatedChangePasswordProvider[UserClass <: AppUser[_]] {
  def changePassword(owner: UserClass, oldPassword: String, password: String, passwordConfirmation: String): Validation[Error, UserClass]
}

trait OAuthUserProvider
