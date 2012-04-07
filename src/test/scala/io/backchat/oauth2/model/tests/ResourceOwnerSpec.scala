package io.backchat.oauth2
package model
package tests

import org.specs2.Specification
import OAuth2Imports._
import scalaz._
import Scalaz._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers

class ResourceOwnerSpec extends AkkaSpecification { def is = sequential ^
  "A resource owner dao should" ^
    "validate a user for registration" ^
      "fails validation for" ^
        "missing login" ! failsRegistrationMissingLogin ^
        "empty login" ! failsRegistrationEmptyLogin ^
        "invalid login" ! failsRegistrationInvalidLogin ^
        "duplicate login" ! failsRegistrationDuplicateLogin ^
        "empty email" ! failsRegistrationEmptyEmail ^
        "invalid email" ! failsRegistrationInvalidEmail ^
        "duplicate email" ! failsRegistrationDuplicateEmail ^
        "empty password" ! failsRegistrationEmptyPassword ^
        "too short password" ! failsRegistrationTooShortPassword ^
        "pasword confirmation mismatch" ! failsRegistrationPasswordMismatch ^
        "a combination of all of the above" !  failsRegistrationAll ^ bt ^
      "register the owner if all validations pass" ! registersOwner ^
      "log a user in" ! logsUserIn ^
  end

  RegisterJodaTimeConversionHelpers()

  def logsUserIn = {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some)
    val res = dao.login("tommy", "blah123", "127.0.0.1")
    conn.close()
    res must beSuccess[ResourceOwner]
  }

  def failsRegistrationDuplicateLogin = {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some)
    val res = dao.register("tommy".some, "tommy2@hiltfiger.no".some, "Tommy2 Hiltfiger".some, "blah123".some, "blah123".some)
    conn.close()
    (res.isFailure must beTrue) and {
      res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login exists already.", "login")).list)
    }
  }

  def failsRegistrationDuplicateEmail = {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some)
    val res = dao.register("tommy2".some, "tommy@hiltfiger.no".some, "Tommy2 Hiltfiger".some, "blah123".some, "blah123".some)
    conn.close()
    (res.isFailure must beTrue) and {
      res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Email exists already.", "email")).list)
    }
  }


  def failsRegistrationEmptyPassword = {
    val conn = MongoConnection()

    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    val res = dao.register(Some("tommy"), "aaa@bbb.com".some, "name".some, "".some, "password".some)
    val result = (res.isFailure must beTrue) and {
      res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Password must be present.", "password")).list)
    }
    conn.close()
    result
  }
  def failsRegistrationTooShortPassword = {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    val res = dao.register(Some("tommy"), "aaa@bbb.com".some, "name".some, "abc".some, "password".some)
    val result = (res.isFailure must beTrue) and {
      res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Password must be at least 6 characters long.", "password")).list)
    }
    conn.close()
    result
  }
  def failsRegistrationPasswordMismatch = {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    val res = dao.register(Some("tommy"), "aaa@bbb.com".some, "name".some, "blah123".some, "password".some)
    val result = (res.isFailure must beTrue) and {
      res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Password must match password confirmation.", "password")).list)
    }
    conn.close()
    result
  }

  def failsRegistrationAll = {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    val res = dao.register(Some(" "), "".some, "".some, "blah123".some, "password".some)
    val exp = nel(
      ValidationError("Login must be present.", "login"),
      ValidationError("Email must be present.", "email"),
      ValidationError("Name must be present.", "name"),
      ValidationError("Password must match password confirmation.", "password"))
    val result = (res.isFailure must beTrue) and {
      res.fail.toOption.get.list must haveTheSameElementsAs(exp.list)
    }
    conn.close()
    result
  }

  def registersOwner = {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    val res = dao.register("tommy".some, "tommy@hiltfiger.no".some, "Tommy Hiltfiger".some, "blah123".some, "blah123".some)
    conn.close()
    res.isSuccess must beTrue and {
      val owner = res.toOption.get
      (owner.login must_== "tommy") and
      (owner.email must_== "tommy@hiltfiger.no") and
      (owner.name must_== "Tommy Hiltfiger") and
      (owner.password.isMatch("blah123") must beTrue)
    }
  }

  def failsRegistrationEmptyLogin = {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    val res = dao.register(Some(" "), "aaa@bbb.com".some, "name".some, "password".some, "password".some)
    val result = (res.isFailure must beTrue) and {
      res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login must be present.", "login")).list)
    }
    conn.close()
    result
  }
  def failsRegistrationMissingLogin = {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    val res = dao.register(None, "aaa@bbb.com".some, "name".some, "password".some, "password".some)
    val result = (res.isFailure must beTrue) and {
      res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login must be present.", "login")).list)
    }
    conn.close()
    result
  }
  def failsRegistrationInvalidLogin = {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    val res = dao.register(Some("a b"), "aaa@bbb.com".some, "name".some, "password".some, "password".some)
    val result = (res.isFailure must beTrue) and {
      res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Login can only contain letters, numbers, underscores and dots.", "login")).list)
    }
    conn.close()
    result
  }
  def failsRegistrationEmptyEmail =  {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    val res = dao.register(Some("tommy"), "".some, "name".some, "password".some, "password".some)
    val result = (res.isFailure must beTrue) and {
      res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Email must be present.", "email")).list)
    }
    conn.close()
    result
  }
  def failsRegistrationInvalidEmail = {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("resource_owner")
    coll.drop()
    val dao = new ResourceOwnerDao(coll)
    val res = dao.register(Some("tommy"), "aaa".some, "name".some, "password".some, "password".some)
    val result = (res.isFailure must beTrue) and {
      res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Email must be a valid email.", "email")).list)
    }
    conn.close()
    result
  }
}
