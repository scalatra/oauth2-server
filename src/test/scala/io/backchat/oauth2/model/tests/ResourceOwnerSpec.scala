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
      "register the owner if all validations pass" ! registration.registersOwner ^ bt ^ bt ^
    "when activating" ^
      "activate the user if the correct token is given" ! pending ^
      "return an already confirmed error when the user is already active" ! pending ^
      "return an invalid token error if the token doesn't exist" ! pending ^ bt ^
    "when logging in" ^
      "log a user in by login/password" ! loggingIn.logsUserIn ^
      "log a user in by remember token" ! pending ^
      "generate a new remember token" ! pending ^
      "fails for invalid credentials" ! pending ^
      "increases login failure count if the user account was valid" ! pending ^
      "increases login success, resets failure count on successful login" ! pending ^ bt ^
    "when resetting" ^
      "resets the password for valid input" ! pending ^
      "returns invalid token error" ! pending ^
      "resets login failure count and reset token on successful login" ! pending ^
  end

  RegisterJodaTimeConversionHelpers()

  def registration = new RegistrationSpecContext
  def loggingIn = new LoginSpecContext

  trait ResourceOwnerSpecContextBase extends After {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)

    def after = {
      conn.close()
    }

  }

  class LoginSpecContext extends ResourceOwnerSpecContextBase {

    def logsUserIn = this {
      dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some)
      dao.login("tommy", "blah123", "127.0.0.1") must beSuccess[ResourceOwner]
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
      val dao = new ResourceOwnerDao(coll)
      val res = dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some)
      res.isSuccess must beTrue and {
        val owner = res.toOption.get
        (owner.login must_== "tommy") and
        (owner.email must_== "tommy@hiltfiger.no") and
        (owner.name must_== "Tommy Hiltfiger") and
        (owner.password.isMatch("blah123") must beTrue)
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
