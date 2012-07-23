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
import command._
import command.Validators.PredicateValidator
import commands.CreatePermissionCommand
import command.Validation

case class Permission(@Key("_id") code: String, name: String, description: String, isSystem: Boolean = false)

trait ModelCommand[TModel <: Product] { self: Command with ValidationSupport ⇒

  def model: TModel
}

trait OAuth2ModelCommand[TModel <: Product] extends Command with ValidationSupport with ModelCommand[TModel]

trait CommandableDao[ObjectType <: Product, ID <: Any] { self: SalatDAO[ObjectType, ID] ⇒

  def execute(cmd: OAuth2ModelCommand[ObjectType]): ValidationNEL[FieldError, ObjectType] = {
    if (cmd.valid == Some(true)) {
      val model = cmd.model
      save(model)
      model.successNel
    } else {
      val f = cmd.errors.map(_.validation) collect {
        case Failure(e) ⇒ e
      }
      nel(f.head, f.tail: _*).fail
    }
  }
}

class PermissionDao(collection: MongoCollection)(implicit system: ActorSystem)
    extends SalatDAO[Permission, String](collection = collection) with CommandableDao[Permission, String] {
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
    def apply(perm: Permission): ValidationNEL[FieldError, Permission] = {
      (code(perm.code).liftFailNel |@|
        name(perm.name).liftFailNel) { (_, _) ⇒ perm }
    }
    /*_*/
  }

  def create(cmd: CreatePermissionCommand): ValidationNEL[FieldError, Permission] = execute(cmd)

}