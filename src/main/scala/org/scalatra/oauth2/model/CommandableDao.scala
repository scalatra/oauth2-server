package org.scalatra
package oauth2
package model

import scalaz._
import Scalaz._
import com.novus.salat.dao.SalatDAO
import command.{ Command, ValidationSupport, FieldError }
import com.novus.salat.Context
import OAuth2Imports._

trait CommandableDao[ObjectType <: Product] {

  def execute[TCommand <: ValidationSupport <% ModelCommand[ObjectType]](cmd: TCommand): ModelValidation[ObjectType] = {
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

  def save(model: ObjectType): Unit
}

abstract class SalatCommandableDao[ObjectType <: Product, ID <: Any](collection: MongoCollection)(implicit mot: Manifest[ObjectType], mid: Manifest[ID], ctx: Context)
    extends SalatDAO[ObjectType, ID](collection) with CommandableDao[ObjectType]
