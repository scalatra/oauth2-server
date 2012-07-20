package org.scalatra
package oauth2
package model
package tests

import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import org.specs2.specification.After
import org.scalatra.oauth2.OAuth2Imports._
import scalaz._
import Scalaz._


class PermissionsSpec extends AkkaSpecification { def is =
  "A permission should" ^
    "not validate when the name is empty" ! validate.failsEmptyName ^ bt ^
    "not validate when the code is" ^
      "empty" ! validate.failsEmptyCode ^
      "invalid format" ! validate.invalidCodeFormat ^
      "not unique" ! validate.duplicateCode ^
  end

  RegisterJodaTimeConversionHelpers()

  def validate = new CreateContext

  trait PermissionSpecContextBase extends After {
    val conn = MongoConnection()
    val coll = conn("oauth_server_test")("permissions")
    coll.drop()
    val dao = new PermissionDao(coll)

    def after = {
      conn.close()
    }

  }

  class CreateContext extends PermissionSpecContextBase {
    def failsEmptyName = this {
      val res = dao.validate(Permission("blah", "", ""))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Name must be present.", "name")).list)
      }
    }

    def failsEmptyCode = this {
      val res = dao.validate(Permission("", "yada", ""))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Code must be present.", "code")).list)
      }
    }

    def invalidCodeFormat = this {
      val res = dao.validate(Permission("***", "yada", ""))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Code can only contain letters, numbers, underscores and hyphens.", "code")).list)
      }
    }

    def duplicateCode = {
      val first = Permission("first-permission", "The first permission", "")
      val second = Permission("first-permission", "The second permission", "")
      dao.save(first)
      val res = dao.validate(second)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Code exists already.", "code")).list)
      }
    }

  }


}
