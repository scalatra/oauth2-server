package org.scalatra
package oauth2
package commands
package tests

import model.{PermissionDao, Permission}
import scalaz._
import Scalaz._
import command.ValidationError
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.commons.MongoDBObject

class PermissionCommandSpec extends AkkaSpecification { def is = sequential ^
  "A create permission command should" ^
    "not validate when the name is empty" ! createPermission.failsEmptyName ^ bt ^
    "not validate when the code is" ^
      "empty" ! createPermission.failsEmptyCode ^
      "invalid format" ! createPermission.invalidCodeFormat ^
      "not unique" ! createPermission.duplicateCode ^
  end

//  RegisterJodaTimeConversionHelpers()
  def createPermission = new CreatePermissionCommandSpecContext

  class CreatePermissionCommandSpecContext extends PermissionSpecContextBase {


    val dao: PermissionDao = new PermissionDao(coll)

    val cmd = new CreatePermissionCommand()

    def failsEmptyName = this {
      cmd.doBinding(Map("code" -> "blah"))
      (cmd.valid must beSome(false)) and {
        cmd.errors.filter(_.rejected.isDefined).map(_.rejected.get) must haveTheSameElementsAs(List(ValidationError("Name must be present.", "name")))
      }
    }

    def failsEmptyCode = this {
      cmd.doBinding(Map("name" -> "yada"))
      (cmd.valid must beSome(false)) and {
        cmd.errors.filter(_.rejected.isDefined).map(_.rejected.get) must haveTheSameElementsAs(List(ValidationError("Code must be present.", "code")))
      }
    }

    def invalidCodeFormat = this {
      cmd.doBinding(Map("name" -> "yada", "code" -> "***"))
      (cmd.valid must beSome(false)) and {
        cmd.errors.filter(_.rejected.isDefined).map(_.rejected.get) must haveTheSameElementsAs(List(ValidationError("Code can only contain letters, numbers, underscores and hyphens.", "code")))
      }
    }

    def duplicateCode = this {
      val first = Permission("first-permission", "The first permission", "")
      dao.save(first)
      cmd.doBinding(Map("name" -> "The second permission", "code" -> "first-permission"))
      (cmd.valid must beSome(false)) and {
        cmd.errors.filter(_.rejected.isDefined).map(_.rejected.get) must haveTheSameElementsAs(List(ValidationError("Code exists already.", "code")))
      }
    }
  }
}
