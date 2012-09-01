package org.scalatra
package oauth2
package commands

import databinding._
import model.{ Permission, fieldNames }
import akka.actor.ActorSystem
import scalaz._
import Scalaz._
import model.Permission
import org.scalatra.validation.{ ValidationFail, FieldName, ValidationError, NotFound ⇒ NotFoundError }
import org.json4s.Formats

trait PermissionModelCommands {
  import org.scalatra.oauth2.model.ModelCommand
  import ModelCommand._
  import BindingSyntax._

  implicit def createPermissionCommand2ModelCommand(cmd: CreatePermissionCommand): ModelCommand[Permission] =
    modelCommand(Permission(~cmd.code.value.toOption, ~cmd.name.value.toOption, ~cmd.description.value.toOption, ~cmd.isSystem.value.toOption))

  implicit def updatePermissionCommand2ModelCommand(cmd: UpdatePermissionCommand): ModelCommand[Permission] =
    modelCommand {
      (cmd.retrieved map {
        _.copy(name = ~cmd.name.value.toOption, description = ~cmd.description.value.toOption, isSystem = ~cmd.isSystem.value.toOption)
      }) | Permission("", ~cmd.name.value.toOption, ~cmd.description.value.toOption, ~cmd.isSystem.value.toOption)
    }
}

abstract class PermissionCommand(oauth: OAuth2Extension)(implicit formats: Formats) extends OAuth2Command[Permission](oauth) {

  def code: Field[String]

  val name: Field[String] = asType[String](fieldNames.name).notBlank

  val description: Field[String] = asType[String](fieldNames.description)

  val isSystem: Field[Boolean] = asBoolean(fieldNames.isSystem)

}

class CreatePermissionCommand(oauth: OAuth2Extension)(implicit formats: Formats) extends PermissionCommand(oauth) {

  val code: Field[String] = asType[String](fieldNames.code).required validateWith { _ ⇒ _ flatMap oauth.permissionDao.validate.code }

}
class UpdatePermissionCommand(oauth: OAuth2Extension)(implicit formats: Formats) extends PermissionCommand(oauth) with IdFromParamsBagCommand {

  lazy val retrieved = oauth.permissionDao.findOneById(~code.value.toOption)

  val code: Field[String] = asType[String](fieldNames.id) validateWith { _ ⇒
    _ flatMap { id ⇒
      println("The id params is: %s" format id)
      oauth.permissionDao.findOneById(id).map(_ ⇒ id.success[ValidationError]) | ValidationError("The permission doesn't exist", FieldName(fieldNames.id), NotFoundError).fail[String]
    }
  }
}