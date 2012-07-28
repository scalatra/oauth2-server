package org.scalatra
package oauth2
package model

import command.{ ValidationSupport, Command, FieldError }
import scalaz._
import Scalaz._
import commands.{ UpdatePermissionCommand, CreatePermissionCommand }

/**
 * Type class for transforming commands to be usable by a dao
 * @tparam TModel
 */
trait ModelCommand[TModel <: Product] {

  def model: TModel
}

object ModelCommand {

  def modelCommand[TCommand <: Command with ValidationSupport, TModel <: Product](factory: ⇒ TModel): ModelCommand[TModel] =
    new ModelCommand[TModel] {
      def model: TModel = factory
    }

}