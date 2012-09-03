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
    modelCommand(Permission(~cmd.code.value, ~cmd.name.value, ~cmd.description.value, ~cmd.isSystem.value))

  implicit def updatePermissionCommand2ModelCommand(cmd: UpdatePermissionCommand): ModelCommand[Permission] =
    modelCommand {
      (cmd.retrieved map {
        _.copy(name = ~cmd.name.value, description = ~cmd.description.value, isSystem = ~cmd.isSystem.value)
      }) | Permission("", ~cmd.name.value, ~cmd.description.value, ~cmd.isSystem.value)
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

  lazy val retrieved = oauth.permissionDao.findOneById(~code.value)

  val code: Field[String] = asType[String](fieldNames.id) validateWith { _ ⇒
    _ flatMap { id ⇒
      oauth.permissionDao.findOneById(id).map(_ ⇒ id.success[ValidationError]) | ValidationError("The permission doesn't exist", FieldName(fieldNames.id), NotFoundError).fail[String]
    }
  }
}