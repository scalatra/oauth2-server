package org.scalatra
package oauth2
package commands
package tests

import model.{PermissionDao, Permission}
import scalaz._
import Scalaz._
import org.scalatra.validation.{FieldName, ValidationError}
import org.json4s._
import JsonDSL._
import com.mongodb.casbah.WriteConcern
import org.junit.runner._
import org.specs2.runner._
import databinding.JsonBindingImports
import util.ParamsValueReaderProperties

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

  import org.scalatra.json.NativeJsonValueReaderProperty
  abstract class PermissionCommandSpecContext(asJson: Boolean)  extends PermissionSpecContextBase with PermissionModelCommands with native.JsonMethods with ParamsValueReaderProperties with NativeJsonValueReaderProperty  {

    def cmd: PermissionCommand
    val dao: PermissionDao = new PermissionDao(coll)

    implicit val jsonFormats: Formats = new OAuth2Formats
    val imports = new JsonBindingImports
  }

  class CreatePermissionCommandSpecContext(asJson: Boolean) extends PermissionCommandSpecContext(asJson) {

    import imports._

    val cmd = new CreatePermissionCommand(OAuth2Extension(system))

    def failsEmptyName = this {
      if (asJson) cmd.bindTo(("code" -> "blah"): JValue) else cmd.bindTo(Map("code" -> "blah"))

      (cmd.isValid must beFalse) and {
        cmd.errors.filter(_.isInvalid).map(_.value.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("Name must be present.", FieldName("name"))))
      }
    }

    def failsEmptyCode = this {
      if (asJson) cmd.bindTo(("name" -> "yada"): JValue) else cmd.bindTo(Map("name" -> "yada"))

      (cmd.isValid must beFalse) and {
        cmd.errors.filter(_.isInvalid).map(_.value.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("Code must be present.", FieldName("code"))))
      }
    }

    def invalidCodeFormat = this {
      if (asJson) cmd.bindTo(("name" -> "yada") ~ ("code" -> "***"): JValue) else cmd.bindTo(Map("name" -> "yada", "code" -> "***"))

      (cmd.isValid must beFalse) and {
        cmd.errors.filter(_.isInvalid).map(_.value.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("Code can only contain letters, numbers, underscores and hyphens.", FieldName("code"))))
      }
    }


    def duplicateCode = this {
      val first = Permission("first-permission", "The first permission", "")
      dao.save(first)
      if (asJson)
        cmd.bindTo(("name" -> "The second permission") ~ ("code" -> "first-permission"): JValue)
      else
        cmd.bindTo(Map("name" -> "The second permission", "code" -> "first-permission"))

      (cmd.isValid must beFalse) and {
        cmd.errors.filter(_.isInvalid).map(_.value.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("Code exists already.", FieldName("code"))))
      }
    }

    def modelOnSuccess = this {
      if (asJson)
        cmd.bindTo(("name" -> "The first permission") ~ ("code" -> "first-permission"): JValue)
      else
        cmd.bindTo(Map("name" -> "The first permission", "code" -> "first-permission"))

      (cmd.isValid must beTrue) and {
        cmd.model must_== Permission("first-permission", "The first permission", null)
      }
    }

    def modelOnFailure = this {
      if (asJson)
        cmd.bindTo(("name" -> "The first permission") ~ ("code" -> ""): JValue)
      else
        cmd.bindTo(Map("name" -> "The first permission", "code" -> ""))

      (cmd.isValid must beFalse) and {
        cmd.model must_== Permission("", "The first permission", null)
      }
    }


  }

  class UpdatePermissionCommandSpecContext(asJson: Boolean) extends PermissionCommandSpecContext(asJson) {

    import imports._
    val cmd = new UpdatePermissionCommand(OAuth2Extension(system))
    val first = Permission("first-permission", "The first permission", "")
    dao.save(first, WriteConcern.Safe)


    def failsEmptyName = this {
      if (asJson) cmd.bindTo(JNothing: JValue, Map("id" -> Seq("first-permission"))) else cmd.bindTo(Map("id" -> "first-permission"))

      (cmd.isValid must beFalse) and {
        cmd.errors.filter(_.isInvalid).map(_.value.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("Name must be present.", FieldName("name"))))
      }
    }


    def nonExistingCode = this {
      if (asJson)
        cmd.bindTo(("name" -> "The second permission"): JValue, Map("id" -> Seq("second-permission")))
      else
        cmd.bindTo(Map("name" -> "The second permission", "id" -> "second-permission"))

      (cmd.isValid must beFalse) and {
        cmd.errors.filter(_.isInvalid).map(_.value.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("The permission doesn't exist.")))
      }
    }

    def modelOnSuccess = this {
      if (asJson)
        cmd.bindTo(("name" -> "The first permission"): JValue, Map("id" -> Seq("first-permission")))
      else
        cmd.bindTo(Map("name" -> "The first permission", "id" -> "first-permission"))

      (cmd.isValid must beTrue) and {
        cmd.model must_== Permission("first-permission", "The first permission", null)
      }
    }

    def modelOnFailure = this {
      if (asJson)
        cmd.bindTo(("name" -> "The first permission"):JValue, Map("id" -> Seq("second-permission")))
      else
        cmd.bindTo(Map("name" -> "The first permission", "id" -> "second-permission"))

      (cmd.isValid must beFalse) and {
        cmd.model must_== Permission("", "The first permission", null)
      }
    }


  }
}
