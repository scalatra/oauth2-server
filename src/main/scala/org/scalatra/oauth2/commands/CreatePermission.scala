package org.scalatra
package oauth2
package commands

import command._
import model.{ Permission, fieldNames }
import org.scalatra.oauth2.OAuth2Imports._
import command.Validators.PredicateValidator
import akka.actor.ActorSystem

abstract class PermissionCommand extends Command with ValidationSupport with CommandValidators {

  def code: ValidatedBinding[String]
  //  lazy val code = {
  //    val b = bind[String](fieldNames.code)
  //    b validate (b.nonEmptyString orElse b.validFormat("""^(\w+|-)([-\w]*)*$""".r, "%s can only contain letters, numbers, underscores and hyphens."))
  //  }
  //
  val name = bind[String](fieldNames.name) validate (nonEmptyString(fieldNames.name))

  val description = bind[String](fieldNames.description)

  val isSystem = bind[Boolean](fieldNames.isSystem)

}

class CreatePermissionCommand(implicit system: ActorSystem) extends PermissionCommand {

  private val oauth = OAuth2Extension(system)

  private[this] def uniqueCode(fieldName: String, collection: MongoCollection): Validator[String] = {
    case Some(ss: String) ⇒
      new PredicateValidator[String](
        fieldName,
        s ⇒ collection.count(Map("_id" -> ss), Map("_id" -> 1)) == 0,
        "%s exists already.").validate(ss)
  }

  val code = bind[String](fieldNames.code) validate {
    case Some(s: String) ⇒ oauth.permissionDao.validate.code(s)
    case None            ⇒ oauth.permissionDao.validate.code(null)
  }

  def model = Permission(code.original, name.original, description.original, isSystem.converted getOrElse false)
}