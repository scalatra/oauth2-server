package org.scalatra
package oauth2
package model

import scalaz._
import Scalaz._
import com.novus.salat.dao.SalatDAO
import command.{ Command, ValidationSupport, FieldError }

trait CommandableDao[ObjectType <: Product, ID <: Any] {
  self: SalatDAO[ObjectType, ID] ⇒

  def execute[TCommand <: ValidationSupport <% ModelCommand[ObjectType]](cmd: TCommand): ValidationNEL[FieldError, ObjectType] = {
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
