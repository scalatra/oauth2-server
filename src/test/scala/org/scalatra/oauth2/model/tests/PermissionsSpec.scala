package org.scalatra
package oauth2
package model
package tests

import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import org.specs2.specification.After
import org.scalatra.oauth2.OAuth2Imports._
import scalaz._
import Scalaz._
import org.scalatra.validation.{ValidationError, FieldName}
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
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



  class CreateContext extends PermissionSpecContextBase {


    val dao: PermissionDao = new PermissionDao(coll)

    def failsEmptyName = this {
      val res = dao.validate(Permission("blah", "", ""))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Name must be present.", FieldName("name"))).list)
      }
    }

    def failsEmptyCode = this {
      val res = dao.validate(Permission("", "yada", ""))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Code must be present.", FieldName("code"))).list)
      }
    }

    def invalidCodeFormat = this {
      val res = dao.validate(Permission("***", "yada", ""))
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Code can only contain letters, numbers, underscores and hyphens.", FieldName("code"))).list)
      }
    }

    def duplicateCode = this {
      val first = Permission("first-permission", "The first permission", "")
      val second = Permission("first-permission", "The second permission", "")
      dao.save(first)
      val res = dao.validate(second)
      (res.isFailure must beTrue) and {
        res.fail.toOption.get.list must haveTheSameElementsAs(nel(ValidationError("Code exists already.", FieldName("code"))).list)
      }
    }

  }


}
