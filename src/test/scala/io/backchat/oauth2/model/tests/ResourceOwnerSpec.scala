package io.backchat.oauth2
package model
package tests

import OAuth2Imports._
import scalaz._
import Scalaz._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import org.specs2.specification.After

class ResourceOwnerSpec extends AkkaSpecification { def is = sequential ^
  "A resource owner dao should" ^
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
      "log a user in by remember token" ! loggingIn.logsUserInFromRemember ^
      "generate a new remember token" ! loggingIn.generatesRememberToken ^
      "fails for invalid credentials" ! loggingIn.failsForInvalidCredentials ^
      "increases login failure count if the user account was valid" ! loggingIn.increasesLoginFailureCount ^
      "increases login success, resets failure count on successful login" ! loggingIn.increasesLoginSuccessCount ^ bt ^
    "when resetting" ^
      "resets the password for valid input" ! passwordReset.resetsPassword ^
      "returns invalid token error" ! passwordReset.invalidTokenError ^
      "resets login failure count and reset token on successful login" ! passwordReset.resetsFailureCountAndTokenOnLogin ^
  end

  RegisterJodaTimeConversionHelpers()

  def registration = new RegistrationSpecContext
  def loggingIn = new LoginSpecContext
  def activation = new ActivationSpecContext
  def passwordReset = new ResetPasswordSpecContext

  trait ResourceOwnerSpecContextBase extends After {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)

    def after = {
      conn.close()
    }

  }

  class ResetPasswordSpecContext extends ResourceOwnerSpecContextBase {
    val registered = dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some).toOption.get
    val toReset = dao.forgot(registered.login.some).toOption.get

    def resetsPassword = this {
      dao.resetPassword(toReset.reset.token, "blah124", "blah124") must beSuccess[ResourceOwner]
    }

    def resetsFailureCountAndTokenOnLogin = this {
      dao.rememberedPassword(toReset, "127.0.0.1")
      val owner = dao.findByLoginOrEmail("tommy").toOption.get.get
      (owner.reset.token must_!= toReset.reset.token) and {
        owner.stats.loginSuccess must be_>(0)
      }
    }

    def invalidTokenError = this {
      dao.resetPassword("plain wrong", "blah124", "blah124") must beFailure[InvalidToken]
    }

  }

  class ActivationSpecContext extends ResourceOwnerSpecContextBase {
    val registered = dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some).toOption.get

    def activatesCorrectToken = this {
      val res = dao.confirm(registered.confirmation.token)
      (res must beSuccess[ResourceOwner]) and {
        val retr = dao.findByLoginOrEmail(registered.login).get
        retr.isConfirmed must beTrue
      }
    }

    def alreadyConfirmed = this {
      dao.confirm(registered.confirmation.token)
      val res = dao.confirm(registered.confirmation.token)
      res must beFailure[AlreadyConfirmed]
    }

    def invalidTokenError = this {
      dao.confirm("plain wrong") must beFailure[InvalidToken]
    }
  }

  class LoginSpecContext extends ResourceOwnerSpecContextBase {

    val registered = dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some).toOption.get
    def logsUserIn = this {
      dao.login("tommy", "blah123", "127.0.0.1") must beSuccess[ResourceOwner]
    }
    def logsUserInFromRemember = this {
      val tok = dao.remember(registered).toOption.get
      dao.loginFromRemember(tok) must beSuccess[ResourceOwner]
    }
    def generatesRememberToken = this {
      dao.remember(registered) must beSuccess[String]
    }
    def failsForInvalidCredentials = this {
      dao.login("tommy", "wrong", "127.0.0.1") must beFailure[Error]
    }
    def increasesLoginFailureCount = this {
      dao.login("tommy", "wrong", "127.0.0.1")
      val usr = dao.findByLoginOrEmail("tommy").toOption.get.get
      usr.stats.loginFailures must be_>(0)
    }
    def increasesLoginSuccessCount = this {
      dao.login("tommy", "blah123", "127.0.0.1") must beSuccess[ResourceOwner]
      val usr = dao.findByLoginOrEmail("tommy").toOption.get.get
      (usr.stats.loginFailures must be_==(0)) and {
        usr.stats.loginSuccess must be_>(0)
      }
    }

  }

  class RegistrationSpecContext extends ResourceOwnerSpecContextBase {

    def failsRegistrationDuplicateLogin = this {
      dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some)
      val res = dao.register("tommy".some, "tommy2@hiltfiger.no".some, "Tommy2 Hiltfiger".some, "blah123".some, "blah123".some)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login exists already.", "login")).list)
      }
    }

    def failsRegistrationDuplicateEmail = this {
      dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some)
      val res = dao.register("tommy2".some, "tommy@hiltfiger.no".some, "Tommy2 Hiltfiger".some, "blah123".some, "blah123".some)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Email exists already.", "email")).list)
      }
    }

    def failsRegistrationEmptyPassword = this {
      val res = dao.register(Some("tommy"), "aaa@bbb.com".some, "name".some, "".some, "password".some)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Password must be present.", "password")).list)
      }
    }
    def failsRegistrationTooShortPassword = this {
      val res = dao.register(Some("tommy"), "aaa@bbb.com".some, "name".some, "abc".some, "password".some)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Password must be at least 6 characters long.", "password")).list)
      }
    }

    def failsRegistrationPasswordMismatch = this {
      val res = dao.register(Some("tommy"), "aaa@bbb.com".some, "name".some, "blah123".some, "password".some)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Password must match password confirmation.", "password")).list)
      }
    }

    def failsRegistrationAll = this {
      val res = dao.register(Some(" "), "".some, "".some, "blah123".some, "password".some)
      val exp = nel(
        ValidationError("Login must be present.", "login"),
        ValidationError("Email must be present.", "email"),
        ValidationError("Name must be present.", "name"),
        ValidationError("Password must match password confirmation.", "password"))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(exp.list)
      }
    }

    def registersOwner = this {
      val res = dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some)
      res.isSuccess must beTrue and {
        val owner = res.toOption.get
        (owner.login must_== "tommy") and
        (owner.email must_== "tommy@hiltfiger.no") and
        (owner.name must_== "Tommy Hiltfiger") and
        (owner.password.isMatch("blah123") must beTrue)
      }
    }

    def ownerIsUnconfirmed = this {
      val res = dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some)
      res.isSuccess must beTrue and {
        val owner = res.toOption.get
        (owner.isConfirmed must beFalse)
      }
    }

    def failsRegistrationEmptyLogin = this {
      val res = dao.register(Some(" "), "aaa@bbb.com".some, "name".some, "password".some, "password".some)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login must be present.", "login")).list)
      }
    }

    def failsRegistrationMissingLogin = this {
      val res = dao.register(None, "aaa@bbb.com".some, "name".some, "password".some, "password".some)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login must be present.", "login")).list)
      }
    }

    def failsRegistrationInvalidLogin = this {
      val res = dao.register(Some("a b"), "aaa@bbb.com".some, "name".some, "password".some, "password".some)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login can only contain letters, numbers, underscores and dots.", "login")).list)
      }
    }

    def failsRegistrationEmptyEmail =  this {
      val res = dao.register(Some("tommy"), "".some, "name".some, "password".some, "password".some)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Email must be present.", "email")).list)
      }
    }

    def failsRegistrationInvalidEmail = this {
      val res = dao.register(Some("tommy"), "aaa".some, "name".some, "password".some, "password".some)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Email must be a valid email.", "email")).list)
      }
    }
  }
}
