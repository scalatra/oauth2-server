package org.scalatra
package oauth2
package model

import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import scalaz._
import Scalaz._
import OAuth2Imports._
import akka.actor.ActorSystem
import databinding._
import org.scalatra.validation.Validators.PredicateValidator
import commands.{ PermissionModelCommands, CreatePermissionCommand }
import org.scalatra.validation.{ ValidationError, Validation }

case class Permission(@Key("_id") code: String, name: String, description: String, isSystem: Boolean = false)

class PermissionDao(collection: MongoCollection)(implicit system: ActorSystem)
    extends SalatCommandableDao[Permission, String](collection = collection) with PermissionModelCommands {
  private val oauth = OAuth2Extension(system)

  oauth.permissions foreach save

  object validate {
    import Validation._

    def name(name: String) = nonEmptyString(fieldNames.name, name)

    private[this] def uniqueCode(fieldName: String, value: ⇒ String, collection: MongoCollection): FieldValidation[String] = {
      def q(s: String) = Map("_id" -> s)
      new PredicateValidator[String](
        fieldName,
        s ⇒ collection.count(q(s), Map("_id" -> 1)) == 0,
        "%s exists already.").validate(value)
    }

    def code(code: String): FieldValidation[String] = {
      for {
        nec ← nonEmptyString(fieldNames.code, code)
        ff ← validFormat(fieldNames.code, nec, """^\w+([-\w]*)*$""".r, "%s can only contain letters, numbers, underscores and hyphens.")
        uniq ← uniqueCode(fieldNames.code, ff, collection)
      } yield uniq
    }

    /*_*/
    def apply(perm: Permission): ValidationNEL[ValidationError, Permission] = {
      (code(perm.code).liftFailNel |@|
        name(perm.name).liftFailNel) { (_, _) ⇒ perm }
    }
    /*_*/
  }

  def create(cmd: CreatePermissionCommand): ValidationNEL[ValidationError, Permission] = execute(cmd)

}