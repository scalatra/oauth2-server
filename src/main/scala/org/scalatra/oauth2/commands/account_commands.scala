package org.scalatra
package oauth2
package commands

import scalaz._
import Scalaz._
import command._
import util.{ MultiMap, MapWithIndifferentAccess, MultiMapHeadView }
import OAuth2Imports._
import model.fieldNames

trait IdCommand[ID <: Any] { self: OAuth2Command ⇒
  def id: ValidatedBinding[ID]

}

trait AccountCommandPart {
  val oauth: OAuth2Extension
}
trait LoginParam extends AccountCommandPart { this: OAuth2Command ⇒
  val login = bind[String](fieldNames.login) validate (oauth.userProvider.validations.login(_: String, None))
}

trait PasswordParam extends AccountCommandPart { this: OAuth2Command ⇒
  val password = bind[String](fieldNames.password) validate (oauth.userProvider.validations.password _)
}

class LoginCommand(oauth: OAuth2Extension) extends OAuth2Command(oauth) {

  // not using the ones from the trait because we don't need to show validation errors
  val login = bind[String](fieldNames.login)
  val password = bind[String](fieldNames.password)
  val remember = bind[String](fieldNames.remember)

}

class RegisterCommand(oauth: OAuth2Extension) extends OAuth2Command(oauth) with LoginParam {

  import oauth.userProvider.validations

  val name = bind[String](fieldNames.name) validate (validations.name _)

  val email = bind[String](fieldNames.email) validate (validations.email(_: String, None))

  val passwordConfirmation =
    bind[String](fieldNames.passwordConfirmation) validate nonEmptyString(fieldNames.passwordConfirmation)

  val password = bind[String](fieldNames.password) validate (
    validations.passwordWithConfirmation(_: String, ~passwordConfirmation.converted))

}

class ForgotCommand(oauth: OAuth2Extension) extends OAuth2Command(oauth) with LoginParam {

}