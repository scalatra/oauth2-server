package org.scalatra
package oauth2
package commands
package tests

import model.{PermissionDao, Permission}
import scalaz._
import Scalaz._
import org.scalatra.validation.{ValidationFail, FieldName, ValidationError, NotFound}
import org.json4s._
import JsonDSL._
import com.mongodb.casbah.WriteConcern
import org.junit.runner._
import org.specs2.runner._
import databinding.JsonBindingImports
import util.{MultiMap, ParamsValueReaderProperties}

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
        cmd.errors.filter(_.isInvalid).map(_.validation.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("Name is required.", FieldName("name"), ValidationFail)))
      }
    }

    def failsEmptyCode = this {
      if (asJson) cmd.bindTo(("name" -> "yada"): JValue) else cmd.bindTo(Map("name" -> "yada"))

      (cmd.isValid must beFalse) and {
        cmd.errors.filter(_.isInvalid).map(_.validation.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("Code is required.", FieldName("code"), ValidationFail)))
      }
    }

    def invalidCodeFormat = this {
      if (asJson) cmd.bindTo(("name" -> "yada") ~ ("code" -> "***"): JValue) else cmd.bindTo(Map("name" -> "yada", "code" -> "***"))

      (cmd.isValid must beFalse) and {
        cmd.errors.filter(_.isInvalid).map(_.validation.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("Code can only contain letters, numbers, underscores and hyphens.", FieldName("code"), ValidationFail)))
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
        cmd.errors.filter(_.isInvalid).map(_.validation.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("Code exists already.", FieldName("code"), ValidationFail)))
      }
    }

    def modelOnSuccess = this {
      if (asJson)
        cmd.bindTo(("name" -> "The first permission") ~ ("code" -> "first-permission"): JValue)
      else
        cmd.bindTo(Map("name" -> "The first permission", "code" -> "first-permission"))

      (cmd.isValid must beTrue) and {
        cmd.model must_== Permission("first-permission", "The first permission", "")
      }
    }

    def modelOnFailure = this {
      if (asJson)
        cmd.bindTo(("name" -> "The first permission") ~ ("code" -> ""): JValue)
      else
        cmd.bindTo(Map("name" -> "The first permission", "code" -> ""))

      (cmd.isValid must beFalse) and {
        cmd.model must_== Permission("", "The first permission", "")
      }
    }


  }

  class UpdatePermissionCommandSpecContext(asJson: Boolean) extends PermissionCommandSpecContext(asJson) {

    import imports._
    val cmd = new UpdatePermissionCommand(OAuth2Extension(system))
    val first = Permission("first-permission", "The first permission", "")
    dao.save(first, WriteConcern.Safe)

    val idParam = MultiMap(Map("id" -> Seq("first-permission")))
    val idParam2 = MultiMap(Map("id" -> Seq("second-permission")))


    def failsEmptyName = this {
      if (asJson) cmd.bindTo(JNothing: JValue, idParam) else cmd.bindTo(Map("id" -> "first-permission"), idParam)

      (cmd.isValid must beFalse) and {
        cmd.errors.filter(_.isInvalid).map(_.validation.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("Name is required.", FieldName("name"), ValidationFail)))
      }
    }


    def nonExistingCode = this {
      if (asJson)
        cmd.bindTo(("name" -> "The second permission"): JValue, idParam2)
      else
        cmd.bindTo(Map("name" -> "The second permission", "id" -> "second-permission"), idParam2)

      (cmd.isValid must beFalse) and {
        cmd.errors.filter(_.isInvalid).map(_.validation.fail.toOption.get) must haveTheSameElementsAs(List(ValidationError("The permission doesn't exist", FieldName("id"), NotFound)))
      }
    }

    def modelOnSuccess = this {
      if (asJson)
        cmd.bindTo(("name" -> "The first permission"): JValue, idParam)
      else
        cmd.bindTo(Map("name" -> "The first permission", "id" -> "first-permission"), idParam)

      (cmd.isValid must beTrue) and {
        cmd.model must_== Permission("first-permission", "The first permission", "")
      }
    }

    def modelOnFailure = this {
      if (asJson)
        cmd.bindTo(("name" -> "The first permission"):JValue, idParam2)
      else
        cmd.bindTo(Map("name" -> "The first permission", "id" -> "second-permission"), idParam2)

      (cmd.isValid must beFalse) and {
        cmd.model must_== Permission("", "The first permission", "")
      }
    }


  }
}
