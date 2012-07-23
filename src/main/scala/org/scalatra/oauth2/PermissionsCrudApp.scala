package org.scalatra
package oauth2

import akka.actor.ActorSystem
import commands.{ PermissionCommand, UpdatePermissionCommand, CreatePermissionCommand }
import model.{ OAuth2ModelCommand, Permission, ApiError, OAuth2Response }
import OAuth2Imports._
import net.liftweb.json._
import command.{ FieldError, ValidationError }
import scalaz._
import Scalaz._

class PermissionsCrudApp(implicit protected val system: ActorSystem) extends OAuth2ServerBaseApp {

  before("/") {
    if (isAnonymous) scentry.authenticate("remember_me")
    if (isAnonymous && scentry.authenticate().isEmpty) unauthenticated()
  }

  def page = ~params.getAs[Int]("page") max 1
  def pageSize = ~params.getAs[Int]("pageSize") max 1

  get("/") {
    val clients = oauth.permissionDao.find(MongoDBObject()).limit(pageSize).skip((page - 1) * pageSize)
    format match {
      case "json" ⇒ OAuth2Response(JArray(clients.toList.map(c ⇒ Extraction.decompose(c))))
      case _      ⇒ jade("permissions", "permissions" -> clients)
    }
  }

  post("/") {
    executeCommand[CreatePermissionCommand]("addPermission")
  }

  put("/:id") {
    executeCommand[UpdatePermissionCommand]("editPermission")
  }

  delete("/:id") {
    oauth.permissionDao.removeById(params("id"))
  }

  private def executeCommand[T <: OAuth2ModelCommand[Permission]](clientRoute: String)(implicit mf: Manifest[T], system: ActorSystem) = {
    val cmd = oauth2Command[T]
    val res = oauth.permissionDao.execute(cmd)
    renderCommandResult(res, cmd.model, clientRoute)
  }
  private def renderCommandResult(result: ValidationNEL[FieldError, Permission], model: Permission, clientRoute: String) = {
    result match {
      case Success(perm) ⇒
        format match {
          case "json" | "xml" ⇒ OAuth2Response(Extraction.decompose(perm))
          case _              ⇒ jade("permissions", "clientRoute" -> "addPermission", "permission" -> perm)
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
            jade("permissions", "clientRoute" -> clientRoute, "permission" -> model, "errors" -> rr.list)
        }

    }
  }

}
