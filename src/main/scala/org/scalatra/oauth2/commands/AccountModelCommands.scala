package org.scalatra
package oauth2
package commands

import model.{ BCryptPassword, Validations, Account, fieldNames }
import command._
import util.{ MultiMap, MapWithIndifferentAccess, MultiMapHeadView }
import scala.util.control.Exception.allCatch
import scalaz._
import Scalaz._
import OAuth2Imports._

trait AccountModelCommands {

  import org.scalatra.oauth2.model.ModelCommand
  import ModelCommand._

  implicit def loginCommand2Model(cmd: LoginCommand): ModelCommand[Account] =
    modelCommand(cmd.retrieved.toOption.get)

  implicit def registerCommand2Model(cmd: RegisterCommand): ModelCommand[Account] =
    modelCommand(Account(~cmd.login.converted, ~cmd.email.converted, ~cmd.name.converted, cmd.password.converted.map(BCryptPassword(_)).orNull))

}

trait LoginParam extends OAuth2CommandPart {
  this: OAuth2Command ⇒

  import oauth.userProvider.validations

  val login = bind[String](fieldNames.login) validate (validations.login(_: String, None))
}

trait PasswordParam extends OAuth2CommandPart {
  this: OAuth2Command ⇒

  import oauth.userProvider.validations

  val password = bind[BCryptPassword](fieldNames.password) validate {
    case s ⇒ s.map(_.success).getOrElse(FieldError(fieldNames.password, "Password is required.").fail)
  }

}

trait RetrievingLoginParam {
  this: OAuth2Command ⇒

  lazy val retrieved: FieldValidation[Account] = {
    val r = login.converted.flatMap(oauth.userProvider.findByLoginOrEmail(_))
    r some (_.success[FieldError]) none FieldError("Not found").fail[Account]
  }

  val login = bind[String](fieldNames.login) validate {
    case s ⇒ for {
      ne ← org.scalatra.command.Validation.nonEmptyString(fieldNames.login, ~s)
      account ← retrieved
    } yield account.login
  }

}

trait ConfirmedPasswordParams extends OAuth2CommandPart {
  this: OAuth2Command ⇒

  import oauth.userProvider.validations

  val passwordConfirmation =
    bind[String](fieldNames.passwordConfirmation) validate nonEmptyString(fieldNames.passwordConfirmation)

  val password = bind[String](fieldNames.password) validate (
    validations.passwordWithConfirmation(_: String, ~passwordConfirmation.converted))
}

trait EmailParam extends OAuth2CommandPart {
  this: OAuth2Command ⇒

  import oauth.userProvider.validations

  val email = bind[String](fieldNames.email) validate (validations.email(_: String, None))
}

trait NameParam extends OAuth2CommandPart {
  this: OAuth2Command ⇒

  import oauth.userProvider.validations

  val name = bind[String](fieldNames.name) validate (validations.name _)
}

class LoginCommand(oauth: OAuth2Extension, getIpAddress: ⇒ String) extends OAuth2Command(oauth) with RetrievingLoginParam {
  val ipAddress = getIpAddress
  val password = {
    bind[BCryptPassword](fieldNames.password) validate { (s: BCryptPassword) ⇒
      for {
        ne ← command.Validation.nonEmptyString(fieldNames.password, s.pwd)
        account ← retrieved
        pwd ← account.password.matches(ne)
      } yield account.password
    }
  }

  val remember = bind[String](fieldNames.remember)

}

class UserFieldsCommand(oauth: OAuth2Extension)
  extends OAuth2Command(oauth) with LoginParam with NameParam with EmailParam

class RegisterCommand(oauth: OAuth2Extension) extends UserFieldsCommand(oauth) with ConfirmedPasswordParams

class ForgotCommand(oauth: OAuth2Extension) extends OAuth2Command(oauth) with RetrievingLoginParam

class ResetCommand(oauth: OAuth2Extension) extends OAuth2Command(oauth) with TokenFromParamsBagCommand with ConfirmedPasswordParams

class OAuthInfoIncompleteCommand(oauth: OAuth2Extension) extends UserFieldsCommand(oauth)

class ActivateAccountCommand(oauth: OAuth2Extension) extends OAuth2Command(oauth) with TokenFromParamsBagCommand