package org.scalatra
package oauth2
package model
package tests

import OAuth2Imports._
import scalaz._
import Scalaz._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import org.specs2.specification.After
import org.scalatra.validation.{ValidationError, FieldName}
import commands._
import java.util.concurrent.atomic.AtomicInteger
import org.junit.runner._
import org.specs2.runner._
import org.json4s.native
import util.ParamsValueReaderProperties
import org.scalatra.json.NativeJsonValueReaderProperty
import databinding.JsonBindingImports

@RunWith(classOf[JUnitRunner])
class AccountSpec extends AkkaSpecification { def is = sequential ^
  "An account dao should" ^
    "when registering" ^
      "validate a user for registration" ^
        "fails validation for" ^
          "missing login" ! registration.failsRegistrationMissingLogin ^
          "empty login" ! registration.failsRegistrationEmptyLogin ^
          "invalid login" ! registration.failsRegistrationInvalidLogin ^
          "duplicate login" ! registration.failsRegistrationDuplicateLogin ^
          "empty email" ! registration.failsRegistrationEmptyEmail ^
          "invalid email" ! registration.failsRegistrationInvalidEmail ^
          "duplicate email" ! registration.failsRegistrationDuplicateEmail ^
          "empty password" ! registration.failsRegistrationEmptyPassword ^
          "too short password" ! registration.failsRegistrationTooShortPassword ^
          "pasword confirmation mismatch" ! registration.failsRegistrationPasswordMismatch ^
          "a combination of all of the above" !  registration.failsRegistrationAll ^ bt ^
      "register the owner if all validations pass" ! registration.registersOwner ^
      "owner is unconfirmed on registration" ! registration.ownerIsUnconfirmed ^ bt ^ bt ^
    "when activating" ^
      "activate the user if the correct token is given" ! activation.activatesCorrectToken ^
      "return an already confirmed error when the user is already active" ! activation.alreadyConfirmed ^
      "return an invalid token error if the token doesn't exist" ! activation.invalidTokenError ^ bt ^
    "when logging in" ^
      "log a user in by login/password" ! loggingIn.logsUserIn ^
//      "log a user in by remember token" ! loggingIn.logsUserInFromRemember ^
//      "generate a new remember token" ! loggingIn.generatesRememberToken ^
      "fails for invalid credentials" ! loggingIn.failsForInvalidCredentials ^
      "increases login failure count if the user account was valid" ! loggingIn.increasesLoginFailureCount ^
      "increases login success, resets failure count on successful login" ! loggingIn.increasesLoginSuccessCount ^ bt ^
    "when resetting" ^
      "resets the password for valid input" ! passwordReset.resetsPassword ^
      "returns invalid token error" ! passwordReset.invalidTokenError ^
      "resets login failure count and reset token on successful login" ! passwordReset.resetsFailureCountAndTokenOnLogin ^
  end

  RegisterJodaTimeConversionHelpers()
//  implicit val formats = new OAuth2Formats

  val oauth = OAuth2Extension(system)
  def registration = new RegistrationSpecContext
  def loggingIn = new LoginSpecContext
  def activation = new ActivationSpecContext
  def passwordReset = new ResetPasswordSpecContext

  trait AccountSpecContextBase extends After with native.JsonMethods with ParamsValueReaderProperties with NativeJsonValueReaderProperty {

    val dao = oauth.userProvider
    dao.collection.drop()
    implicit val jsonFormats = new OAuth2Formats
    
    val imports = new JsonBindingImports

    def after = {

    }

  }

  class ResetPasswordSpecContext extends AccountSpecContextBase {
    import imports._
    val cmd = {
      val c = new RegisterCommand(oauth, "127.0.0.1")
      c.bindTo(
        Map(
          "login" -> "tommy",
          "email" -> "tommy@hiltfiger.no",
          "password" -> "blah123",
          "passwordConfirmation" -> "blah123",
          "name" -> "Tommy Hiltfiger")
      )
      c
    }
    val registered = dao.register(cmd).toOption.get
    dao.save(registered.copy(confirmedAt = DateTime.now))
    val forgot = {
      val c = new ForgotCommand(oauth)
      c.bindTo(Map("login" -> registered.login))
      c
    }
    val forgotten = dao.forgot(forgot).toOption.get

    def resetsPassword = this {
      val reset = {
        val c = new ResetCommand(oauth, "127.0.0.1")
        c.bindTo(Map("password" -> "blah124", "passwordConfirmation" -> "blah124"), Map("token" -> Seq(forgotten.reset.token)))
        c
      }
      dao.resetPassword(reset) must beSuccess[Account]
    }

    def resetsFailureCountAndTokenOnLogin = this {
      val login = {
        val c = new LoginCommand(oauth, "127.0.0.1")
        c.bindTo(Map("login" -> "tommy", "password" -> "blah123"))
        c
      }
      dao.login(login) must beSuccess and {
      val owner = dao.findByLoginOrEmail("tommy").toOption.get.get
        (owner.reset.token must_!= forgotten.reset.token) and {
          owner.stats.loginSuccess must be_>(0)
        }
      }
    }

    def invalidTokenError = this {
      val reset = {
        val c = new ResetCommand(oauth, "127.0.0.1")
        c.bindTo(Map("password" -> "blah124", "passwordConfirmation" -> "blah124"), Map("token" -> Seq("plain wrong")))
        c
      }
      dao.resetPassword(reset) must beFailure[ValidationError]
    }

  }

  class ActivationSpecContext extends AccountSpecContextBase {
    import imports._
    val cmd = {
      val c = new RegisterCommand(oauth, "127.0.0.1")
      c.bindTo(
        Map(
          "login" -> "tommy",
          "email" -> "tommy@hiltfiger.no",
          "password" -> "blah123",
          "passwordConfirmation" -> "blah123",
          "name" -> "Tommy Hiltfiger")
      )
      c
    }
    val registered = dao.register(cmd).toOption.get



    def activatesCorrectToken = this {
      val confirm = {
        val c = new ActivateAccountCommand(oauth, "127.0.0.1")
        c.bindTo(Map.empty[String, String], Map("token" -> Seq(registered.confirmation.token)))
        c
      }
      val res = dao.confirm(confirm)
      (res must beSuccess[Account]) and {
        val retr = dao.findByLoginOrEmail(registered.login).get
        retr.isConfirmed must beTrue
      }
    }

    def alreadyConfirmed = this {
      val confirm = {
        val c = new ActivateAccountCommand(oauth, "127.0.0.1")
        c.bindTo(Map.empty[String, String], Map("token" -> Seq(registered.confirmation.token)))
        c
      }
      dao.confirm(confirm)
      val res = dao.confirm(confirm)
      res must beFailure[ValidationError]
    }

    def invalidTokenError = this {
      val confirm = {
        val c = new ActivateAccountCommand(oauth, "127.0.0.1")
        c.bindTo(Map.empty[String, String], Map("token" -> Seq("plain wrong")))
        c
      }
      dao.confirm(confirm) must beFailure[ValidationError]
    }
  }

  class LoginSpecContext extends AccountSpecContextBase {

    import imports._
    val cmd = {
      val c = new RegisterCommand(oauth, "127.0.0.1")
      c.bindTo(
        Map(
          "login" -> "tommy",
          "email" -> "tommy@hiltfiger.no",
          "password" -> "blah123",
          "passwordConfirmation" -> "blah123",
          "name" -> "Tommy Hiltfiger")
      )
      c
    }
    val registered = dao.register(cmd).toOption.get
    dao.save(registered.copy(confirmedAt = DateTime.now))

    def loginCommand(login: String, password: String, ipAddress: String) = {
      val c = new LoginCommand(oauth, ipAddress)
      c.bindTo(Map("login" -> login, "password" -> password))
      c
    }

    def logsUserIn = this {
      dao.login(loginCommand("tommy", "blah123", "127.0.0.1")) must beSuccess[Account]
    }
//    def logsUserInFromRemember = this {
//      val remember = {
//        val c = new LoginFromRememberCommand(oauth)
//        c.bindTo(Map("token" -> registered.remember.token))
//        c
//      }
//      val tok = dao.remember(registered).toOption.get
//      dao.loginFromRemember(tok) must beSuccess[Account]
//    }
//    def generatesRememberToken = this {
//      dao.remember(registered) must beSuccess[String]
//    }
    def failsForInvalidCredentials = this {
      dao.login(loginCommand("tommy", "wrong", "127.0.0.1")) must beFailure[Error]
    }
    def increasesLoginFailureCount = this {
      dao.login(loginCommand("tommy", "meh", "127.0.0.1"))
      val usr = dao.findByLoginOrEmail("tommy").toOption.get.get
      usr.stats.loginFailures must be_>(0)
    }
    def increasesLoginSuccessCount = this {
      dao.login(loginCommand("tommy", "blah123", "127.0.0.1")) must beSuccess[Account] and {
        val usr = dao.findByLoginOrEmail("tommy").toOption.get.get
        (usr.stats.loginFailures must be_==(0)) and {
          usr.stats.loginSuccess must be_>(0)
        }
      }
    }

  }

  class RegistrationSpecContext extends AccountSpecContextBase {
    import imports._
    def reg(login: String, email: String, name: String, password: String, passwordConfirmation: String) = {
      val c = new RegisterCommand(oauth, "127.0.0.1")
      c.bindTo(
        Map(
          "login" -> login,
          "email" -> email,
          "password" -> password,
          "passwordConfirmation" -> passwordConfirmation,
          "name" -> name)
      )
      c
    }
    def failsRegistrationDuplicateLogin = this {
      dao.register(reg("tommy", "tommy@hiltfiger.no", "Tommy Hiltfiger", "blah123", "blah123"))
      val res = dao.register(reg("tommy", "tommy2@hiltfiger.no", "Tommy2 Hiltfiger", "blah123", "blah123"))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login exists already.", FieldName("login"), NotUnique)).list)
      }
    }

    def failsRegistrationDuplicateEmail = this {
      dao.register(reg("tommy", "tommy@hiltfiger.no", "Tommy Hiltfiger", "blah123", "blah123"))
      val res = dao.register(reg("tommy2", "tommy@hiltfiger.no", "Tommy2 Hiltfiger", "blah123", "blah123"))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Email exists already.", FieldName("email"), NotUnique)).list)
      }
    }

    def failsRegistrationEmptyPassword = this {
      val res = dao.register(reg("tommy", "aaa@bbb.com", "name", "", "password"))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Password is required.", FieldName("password"))).list)
      }
    }
    def failsRegistrationTooShortPassword = this {
      val res = dao.register(reg("tommy", "aaa@bbb.com", "name", "abc", "password"))
       (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Password must be at least 6 characters long.", FieldName("password"))).list)
      }
    }

    def failsRegistrationPasswordMismatch = this {
      val res = dao.register(reg("tommy", "aaa@bbb.com", "name", "blah123", "password"))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Password must match password confirmation.", FieldName("password"))).list)
      }
    }

    def failsRegistrationAll = this {
      val res = dao.register(reg(" ", "", "", "blah123", "password"))
      val exp = nel(
        ValidationError("Login must be present.", FieldName("login")),
        ValidationError("Email is required.", FieldName("email")),
        ValidationError("Name is required.", FieldName("name")),
        ValidationError("Password must match password confirmation.", FieldName("password")))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(exp.list)
      }
    }

    def registersOwner = this {
      val res = dao.register(reg("tommy", "tommy@hiltfiger.no", "Tommy Hiltfiger", "blah123", "blah123"))
      res.isSuccess must beTrue and {
        val owner = res.toOption.get
        (owner.login must_== "tommy") and
        (owner.email must_== "tommy@hiltfiger.no") and
        (owner.name must_== "Tommy Hiltfiger") and
        (owner.password.isMatch("blah123") must beTrue)
      }
    }

    def ownerIsUnconfirmed = this {
      val res = dao.register(reg("tommy", "tommy@hiltfiger.no", "Tommy Hiltfiger", "blah123", "blah123"))
      res.isSuccess must beTrue and {
        val owner = res.toOption.get
        (owner.isConfirmed must beFalse)
      }
    }

    def failsRegistrationEmptyLogin = this {
      val res = dao.register(reg(" ", "tommy@hiltfiger.no", "Tommy Hiltfiger", "blah123", "blah123"))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login must be present.", FieldName("login"))).list)
      }
    }

    def failsRegistrationMissingLogin = this {
      val res = dao.register(reg(null, "tommy@hiltfiger.no", "Tommy Hiltfiger", "blah123", "blah123"))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login must be present.", FieldName("login"))).list)
      }
    }

    def failsRegistrationInvalidLogin = this {
      val res = dao.register(reg("a b", "tommy@hiltfiger.no", "Tommy Hiltfiger", "blah123", "blah123"))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login can only contain letters, numbers, underscores and dots.", FieldName("login"))).list)
      }
    }

    def failsRegistrationEmptyEmail =  this {
      val res = dao.register(reg("tommy", "", "Tommy Hiltfiger", "blah123", "blah123"))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Email is required.", FieldName("email"))).list)
      }
    }

    def failsRegistrationInvalidEmail = this {
      val res = dao.register(reg("tommy", "bad", "Tommy Hiltfiger", "blah123", "blah123"))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Email must be a valid email.", FieldName("email"))).list)
      }
    }
  }
}
