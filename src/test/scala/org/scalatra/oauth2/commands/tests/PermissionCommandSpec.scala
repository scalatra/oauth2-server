package org.scalatra
package oauth2
package commands
package tests

import model.{PermissionDao, Permission}
import scalaz._
import Scalaz._
import command.{SimpleError, ValidationError}
import net.liftweb.json._
import JsonDSL._
import com.mongodb.casbah.WriteConcern
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class PermissionCommandSpec extends AkkaSpecification { def is = sequential ^
  "A create permission command should" ^
     "when binding from params" ^ createPermissionSpecs(asJson = false) ^
     "when binding from json" ^ createPermissionSpecs(asJson = true)  ^ p ^
  "An update permission command should" ^
      "when binding from params" ^ updatePermissionSpecs(asJson = false) ^
      "when binding from json" ^ updatePermissionSpecs(asJson = true) ^
  end

  def createPermissionSpecs(asJson: Boolean) =
    "not validate when the name is empty" ! createPermission(asJson = asJson).failsEmptyName ^ bt ^
    "not validate when the code is" ^
      "empty" ! createPermission(asJson = asJson).failsEmptyCode ^
      "invalid format" ! createPermission(asJson = asJson).invalidCodeFormat ^
      "not unique" ! createPermission(asJson = asJson).duplicateCode ^ bt ^
    "return a model" ^
      "with the converted values on success" ! createPermission(asJson = asJson).modelOnSuccess ^
      "with the converted values on failure" ! createPermission(asJson = asJson).modelOnFailure ^ bt

  def updatePermissionSpecs(asJson: Boolean) =
    "not validate when the name is empty" ! updatePermission(asJson = asJson).failsEmptyName ^ bt ^
    "not validate when the code is" ^
      "not exists" ! updatePermission(asJson = asJson).nonExistingCode ^ bt ^
    "return a model" ^
      "with the converted values on success" ! updatePermission(asJson = asJson).modelOnSuccess ^
      "with the converted values on failure" ! updatePermission(asJson = asJson).modelOnFailure ^ bt

  def createPermission(asJson: Boolean) = new CreatePermissionCommandSpecContext(asJson)
  def updatePermission(asJson: Boolean) = new UpdatePermissionCommandSpecContext(asJson)

  abstract class PermissionCommandSpecContext(asJson: Boolean)  extends PermissionSpecContextBase with PermissionModelCommands {

    def cmd: PermissionCommand
    val dao: PermissionDao = new PermissionDao(coll)

    implicit val formats: Formats = new OAuth2Formats

  }

  class CreatePermissionCommandSpecContext(asJson: Boolean) extends PermissionCommandSpecContext(asJson) {


    val cmd = new CreatePermissionCommand(OAuth2Extension(system))

    def failsEmptyName = this {
      if (asJson) cmd.doBinding(json = ("code" -> "blah"): JValue) else cmd.doBinding(Map("code" -> "blah"))

      (cmd.valid must beSome(false)) and {
        cmd.errors.filter(_.rejected.isDefined).map(_.rejected.get) must haveTheSameElementsAs(List(ValidationError("Name must be present.", "name")))
      }
    }

    def failsEmptyCode = this {
      if (asJson) cmd.doBinding(json = ("name" -> "yada"): JValue) else cmd.doBinding(Map("name" -> "yada"))

      (cmd.valid must beSome(false)) and {
        cmd.errors.filter(_.rejected.isDefined).map(_.rejected.get) must haveTheSameElementsAs(List(ValidationError("Code must be present.", "code")))
      }
    }

    def invalidCodeFormat = this {
      if (asJson) cmd.doBinding(json = ("name" -> "yada") ~ ("code" -> "***")) else cmd.doBinding(Map("name" -> "yada", "code" -> "***"))

      (cmd.valid must beSome(false)) and {
        cmd.errors.filter(_.rejected.isDefined).map(_.rejected.get) must haveTheSameElementsAs(List(ValidationError("Code can only contain letters, numbers, underscores and hyphens.", "code")))
      }
    }


    def duplicateCode = this {
      val first = Permission("first-permission", "The first permission", "")
      dao.save(first)
      if (asJson)
        cmd.doBinding(json = ("name" -> "The second permission") ~ ("code" -> "first-permission"))
      else
        cmd.doBinding(Map("name" -> "The second permission", "code" -> "first-permission"))

      (cmd.valid must beSome(false)) and {
        cmd.errors.filter(_.rejected.isDefined).map(_.rejected.get) must haveTheSameElementsAs(List(ValidationError("Code exists already.", "code")))
      }
    }

    def modelOnSuccess = this {
      if (asJson)
        cmd.doBinding(json = ("name" -> "The first permission") ~ ("code" -> "first-permission"))
      else
        cmd.doBinding(Map("name" -> "The first permission", "code" -> "first-permission"))

      (cmd.valid must beSome(true)) and {
        cmd.model must_== Permission("first-permission", "The first permission", "")
      }
    }

    def modelOnFailure = this {
      if (asJson)
        cmd.doBinding(json = ("name" -> "The first permission") ~ ("code" -> ""))
      else
        cmd.doBinding(Map("name" -> "The first permission", "code" -> ""))

      (cmd.valid must beSome(false)) and {
        cmd.model must_== Permission("", "The first permission", "")
      }
    }


  }

  class UpdatePermissionCommandSpecContext(asJson: Boolean) extends PermissionCommandSpecContext(asJson) {

    val cmd = new UpdatePermissionCommand(OAuth2Extension(system))
    val first = Permission("first-permission", "The first permission", "")
    dao.save(first, WriteConcern.Safe)


    def failsEmptyName = this {
      if (asJson) cmd.doBinding(Map("id" -> "first-permission"), jsonOnly = true) else cmd.doBinding(Map("id" -> "first-permission"))

      (cmd.valid must beSome(false)) and {
        cmd.errors.filter(_.rejected.isDefined).map(_.rejected.get) must haveTheSameElementsAs(List(ValidationError("Name must be present.", "name")))
      }
    }


    def nonExistingCode = this {
      if (asJson)
        cmd.doBinding(Map("id" -> "second-permission"), ("name" -> "The second permission"))
      else
        cmd.doBinding(Map("name" -> "The second permission", "id" -> "second-permission"))

      (cmd.valid must beSome(false)) and {
        cmd.errors.filter(_.rejected.isDefined).map(_.rejected.get) must haveTheSameElementsAs(List(SimpleError("The permission doesn't exist.")))
      }
    }

    def modelOnSuccess = this {
      if (asJson)
        cmd.doBinding(Map("id" -> "first-permission"), ("name" -> "The first permission"))
      else
        cmd.doBinding(Map("name" -> "The first permission", "id" -> "first-permission"))

      (cmd.valid must beSome(true)) and {
        cmd.model must_== Permission("first-permission", "The first permission", "")
      }
    }

    def modelOnFailure = this {
      if (asJson)
        cmd.doBinding(Map("id" -> "second-permission"), ("name" -> "The first permission"))
      else
        cmd.doBinding(Map("name" -> "The first permission", "id" -> "second-permission"))

      (cmd.valid must beSome(false)) and {
        cmd.model must_== Permission("", "The first permission", null)
      }
    }


  }
}
