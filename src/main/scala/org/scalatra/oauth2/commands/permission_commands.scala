package org.scalatra
package oauth2
package commands

import command._
import model.{ Permission, fieldNames }
import akka.actor.ActorSystem
import scalaz._
import Scalaz._
import model.Permission

trait PermissionModelCommands {
  import org.scalatra.oauth2.model.ModelCommand
  import ModelCommand._

  implicit def createPermissionCommand2ModelCommand(cmd: CreatePermissionCommand): ModelCommand[Permission] =
    modelCommand(Permission(~cmd.code.converted, ~cmd.name.converted, ~cmd.description.converted, ~cmd.isSystem.converted))

  implicit def updatePermissionCommand2ModelCommand(cmd: UpdatePermissionCommand): ModelCommand[Permission] =
    modelCommand {
      (cmd.retrieved map {
        _.copy(name = ~cmd.name.converted, description = ~cmd.description.converted, isSystem = ~cmd.isSystem.converted)
      }) | Permission("", cmd.name.original, cmd.description.original, ~cmd.isSystem.converted)
    }
}

abstract class PermissionCommand(oauth: OAuth2Extension) extends OAuth2Command(oauth) {

  def code: ValidatedBinding[String]

  val name = bind[String](fieldNames.name).withBinding(b ⇒ b validate b.nonEmptyString)

  val description = bind[String](fieldNames.description)

  val isSystem = bind[Boolean](fieldNames.isSystem)

}

class CreatePermissionCommand(oauth: OAuth2Extension) extends PermissionCommand(oauth) {

  val code = bind[String](fieldNames.code) validate {
    case s ⇒ oauth.permissionDao.validate.code(~s)
  }

}
class UpdatePermissionCommand(oauth: OAuth2Extension) extends PermissionCommand(oauth) with IdFromParamsBagCommand {

  lazy val retrieved = oauth.permissionDao.findOneById(~code.converted)

  val code: ValidatedBinding[String] = bind[String]("id") validate {
    case s ⇒
      (retrieved.map(_ ⇒ (~s).success[FieldError]) | SimpleError("The permission doesn't exist.").fail[String]): FieldValidation[String]
  }
}