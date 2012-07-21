package org.scalatra
package oauth2
package commands

import command._
import model.{ OAuth2ModelCommand, Permission, fieldNames }
import akka.actor.ActorSystem
import scalaz._
import Scalaz._
import model.Permission

abstract class PermissionCommand(implicit system: ActorSystem) extends OAuth2ModelCommand[Permission] with CommandValidators {

  protected val oauth = OAuth2Extension(system)

  def code: ValidatedBinding[String]

  val name = bind[String](fieldNames.name).withBinding(b ⇒ b validate b.nonEmptyString)

  val description = bind[String](fieldNames.description)

  val isSystem = bind[Boolean](fieldNames.isSystem)

  def model = Permission(~code.converted, ~name.converted, ~description.converted, ~isSystem.converted)
}

class CreatePermissionCommand(implicit system: ActorSystem) extends PermissionCommand()(system) {

  val code = bind[String](fieldNames.code) validate {
    case s ⇒ oauth.permissionDao.validate.code(~s)
  }

}
class UpdatePermissionCommand(implicit system: ActorSystem) extends PermissionCommand()(system) {

  private lazy val retrieved = oauth.permissionDao.findOneById(~code.converted)

  val code: ValidatedBinding[String] = bind[String]("id") validate {
    case s ⇒
      (retrieved.map(_ ⇒ (~s).success[FieldError]) | SimpleError("The permission doesn't exist").fail[String]): FieldValidation[String]
  }

  override def model: Permission = {
    (retrieved map {
      _.copy(name = ~name.converted, description = ~description.converted, isSystem = ~isSystem.converted)
    }) | Permission(code.original, name.original, description.original, ~isSystem.converted)
  }
}