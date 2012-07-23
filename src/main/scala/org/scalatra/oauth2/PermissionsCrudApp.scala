package org.scalatra
package oauth2

import akka.actor.ActorSystem
import commands.{ PermissionCommand, UpdatePermissionCommand, CreatePermissionCommand }
import model._
import model.OAuth2Response
import model.Permission
import OAuth2Imports._
import net.liftweb.json._
import command.{ FieldError, ValidationError }
import scalaz._
import Scalaz._
import scalaz.Failure
import command.ValidationError
import scalaz.Success
import com.novus.salat.dao.SalatDAO

abstract class SalatCrudApp[ObjectType <: AnyRef, ID <: Any](implicit mf: Manifest[ObjectType], protected val system: ActorSystem) extends OAuth2ServerBaseApp {
  def dao: SalatDAO[ObjectType, ID] with CommandableDao[ObjectType, ID]
  lazy val viewName: String = mf.erasure.getSimpleName.underscore.pluralize


  before("/") {
    if (isAnonymous) scentry.authenticate("remember_me")
    if (isAnonymous && scentry.authenticate().isEmpty) unauthenticated()
  }

  def page = ~params.getAs[Int]("page") max 1
  def pageSize = ~params.getAs[Int]("pageSize") max 1

  get("/") {
    val clients = dao.find(MongoDBObject()).limit(pageSize).skip((page - 1) * pageSize)
    format match {
      case "json" ⇒ OAuth2Response(JArray(clients.toList.map(c ⇒ Extraction.decompose(c))))
      case _      ⇒ jade("permissions", "permissions" -> clients)
    }
  }

  post("/") {
    executeCreateCommand
  }

  put("/:id") {
    executeUpdateCommand
  }

  delete("/:id") {
    dao.removeById(params("id"))
  }

  protected def executeCreateCommand: Any
  protected def executeUpdateCommand: Any

  protected def executeCommand[T <: OAuth2ModelCommand[Permission]](clientRoute: String)(implicit mf: Manifest[T], system: ActorSystem) = {
    val cmd = oauth2Command[T]
    val res = dao.execute(cmd)
    renderCommandResult(res, cmd.model, clientRoute)
  }

  private def renderCommandResult(result: ValidationNEL[FieldError, Permission], model: Permission, clientRoute: String) = {
    result match {
      case Success(perm) ⇒
        format match {
          case "json" | "xml" ⇒ OAuth2Response(Extraction.decompose(perm))
          case _              ⇒ jade(viewName, "clientRoute" -> clientRoute, "model" -> perm)
        }

      case Failure(errs) ⇒
        val rr = (errs map {
          case e: ValidationError ⇒ ApiError(e.field, e.message)
          case e                  ⇒ ApiError(e.message)
        })
        format match {
          case "json" | "xml" ⇒
            OAuth2Response(Extraction.decompose(model), rr.list.map(_.toJValue))
          case _ ⇒
            jade(viewName, "clientRoute" -> clientRoute, "model" -> model, "errors" -> rr.list)
        }

    }
  }
}

class PermissionsCrudApp(implicit system: ActorSystem) extends SalatCrudApp[Permission, String] {

  val dao = oauth.permissionDao

  protected def executeCreateCommand: Any = executeCommand[CreatePermissionCommand]("addPermission")

  protected def executeUpdateCommand: Any = executeCommand[UpdatePermissionCommand]("editPermission")
}
