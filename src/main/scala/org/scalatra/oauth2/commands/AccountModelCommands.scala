package org.scalatra
package oauth2
package commands

import model._
import databinding._
import scalaz._
import Scalaz._
import OAuth2Imports._
import model.Account
import org.scalatra.validation.ValidationError
import org.json4s.Formats

trait AccountModelCommands {

  import org.scalatra.oauth2.model.ModelCommand
  import ModelCommand._
  import BindingSyntax._

  implicit def loginCommand2Model(cmd: LoginCommand): ModelCommand[Account] =
    modelCommand(cmd.retrieved.toOption.get)

  implicit def registerCommand2Model(cmd: RegisterCommand): ModelCommand[Account] = {
    modelCommand(Account(~cmd.login.value.toOption, ~cmd.email.value.toOption, ~cmd.name.value.toOption, cmd.password.value.toOption.map(BCryptPassword(_)).orNull))
  }

}

trait LoginParam extends OAuth2CommandPart { this: OAuth2Command[_] ⇒

  import oauth.userProvider.validations

  val login: Field[String] = bind[String](fieldNames.login).validateWith(_ ⇒ _ flatMap (validations.login(_, None)))
}

trait PasswordParam extends OAuth2CommandPart { this: OAuth2Command[_] ⇒

  val password: Field[BCryptPassword] = asType[BCryptPassword](fieldNames.password).required

}

trait RetrievingLoginParam { this: OAuth2Command[_] ⇒

  lazy val retrieved: FieldValidation[Account] = {
    val r = login.value.toOption.flatMap(s ⇒ oauth.userProvider.findByLoginOrEmail(s))
    r some (_.success[ValidationError]) none ValidationError("Not found").fail[Account]
  }

  val login: Field[String] =
    asType[String](fieldNames.login).notBlank.validateWith(_ ⇒ _ flatMap (_ ⇒ retrieved.map(_.login)))

}

trait HasRequestIp { this: OAuth2Command[_] ⇒
  def ipAddress: String
}

trait ConfirmedPasswordParams extends OAuth2CommandPart { this: OAuth2Command[_] ⇒

  val passwordConfirmation: Field[String] =
    asType[String](fieldNames.passwordConfirmation).notBlank

  val password: Field[String] =
    asType[String](fieldNames.password).notBlank.validForConfirmation("passwordConfirmation", ~passwordConfirmation.value.toOption)

}

trait EmailParam extends OAuth2CommandPart { this: OAuth2Command[_] ⇒

  import oauth.userProvider.validations

  val email: Field[String] = asType[String](fieldNames.email).validateWith(_ ⇒ _ flatMap (validations.email(_: String, None)))
}

trait NameParam extends OAuth2CommandPart { this: OAuth2Command[_] ⇒

  import oauth.userProvider.validations

  val name: Field[String] = asType[String](fieldNames.name) validateWith (_ ⇒ _ flatMap (validations.name _))
}

class LoginCommand(oauth: OAuth2Extension, getIpAddress: ⇒ String)(implicit formats: Formats) extends OAuth2Command[AuthSession](oauth) with RetrievingLoginParam with HasRequestIp {
  val ipAddress = getIpAddress
  val password: Field[BCryptPassword] = {
    asType[BCryptPassword](fieldNames.password) validateWith (_ ⇒ {
      _ flatMap { s ⇒
        for {
          ne ← org.scalatra.validation.Validation.nonEmptyString(fieldNames.password, s.pwd)
          account ← retrieved
          pwd ← account.password.matches(ne)
        } yield pwd
      }
    })
  }

  val remember: Field[Boolean] = asBoolean(fieldNames.remember)

}

class UserFieldsCommand[S](oauth: OAuth2Extension)(implicit mf: Manifest[S], formats: Formats)
  extends OAuth2Command[S](oauth) with LoginParam with NameParam with EmailParam

class RegisterCommand(oauth: OAuth2Extension, getIpAddress: ⇒ String)(implicit formats: Formats) extends UserFieldsCommand[AuthSession](oauth) with ConfirmedPasswordParams with HasRequestIp {
  val ipAddress: String = getIpAddress
}

class ForgotCommand(oauth: OAuth2Extension)(implicit formats: Formats) extends OAuth2Command[Account](oauth) with RetrievingLoginParam

class ResetCommand(oauth: OAuth2Extension, getIpAddress: ⇒ String)(implicit formats: Formats) extends OAuth2Command[AuthSession](oauth) with TokenFromParamsBagCommand with ConfirmedPasswordParams with HasRequestIp {
  val ipAddress: String = getIpAddress
}

class OAuthInfoIncompleteCommand(oauth: OAuth2Extension, getIpAddress: ⇒ String)(implicit formats: Formats) extends UserFieldsCommand[AuthSession](oauth) with HasRequestIp {
  val ipAddress: String = getIpAddress
}

class ActivateAccountCommand(oauth: OAuth2Extension, getIpAddress: ⇒ String)(implicit formats: Formats) extends OAuth2Command[AuthSession](oauth) with TokenFromParamsBagCommand with HasRequestIp {
  val ipAddress: String = getIpAddress
}

class ChangePasswordCommand(oauth: OAuth2Extension)(implicit val user: Account, formats: Formats) extends OAuth2Command[Account](oauth) with ConfirmedPasswordParams {
  val oldPassword: Field[BCryptPassword] =
    asType[BCryptPassword]("oldPassword") validateWith (_ ⇒ {
      _ flatMap { s ⇒
        for {
          ne ← org.scalatra.validation.Validation.nonEmptyString("oldPassword", s.pwd)
          pwd ← user.password.matches(ne)
        } yield pwd
      }
    })
}