package org.scalatra
package oauth2

import akka.actor.ActorSystem
import commands.{ CreatePermissionCommand, PermissionCommand }
import model.{OAuth2ModelCommand, ApiError, OAuth2Response}
import OAuth2Imports._
import net.liftweb.json._
import command.{ ValidationError, Command, CommandSupport }
import scalaz._
import Scalaz._

class PermissionsCrudApp(implicit protected val system: ActorSystem) extends OAuth2ServerBaseApp with CommandSupport {

  before("/") {
    if (isAnonymous) scentry.authenticate("remember_me")
    if (isAnonymous && scentry.authenticate().isEmpty) unauthenticated()
  }

  def page = params.getOrElse("page", "1").toInt max 1
  def pageSize = params.getOrElse("pageSize", "20").toInt max 1

  get("/") {
    val clients = oauth.permissionDao.find(MongoDBObject()).limit(pageSize).skip((page - 1) * pageSize)
    format match {
      case "json" ⇒ OAuth2Response(JArray(clients.toList.map(c ⇒ Extraction.decompose(c))))
      case _      ⇒ jade("permissions", "permissions" -> clients)
    }
  }

  post("/") {
    val cmd = oauth2Command[CreatePermissionCommand]
    oauth.permissionDao.execute(cmd) match {
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
        val model = cmd.model
        format match {
          case "json" | "xml" ⇒ OAuth2Response(Extraction.decompose(model), rr.list.map(_.toJValue))
          case _              ⇒ jade("permissions", "clientRoute" -> "addPermission", "permission" -> model, "errors" -> rr.list)
        }

    }
  }

  /**
   * Create and bind a [[org.scalatra.command.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  def oauth2Command[T <: OAuth2ModelCommand[_]](implicit mf: Manifest[T], system: ActorSystem): T = {
    commandOption[T] getOrElse {
      val newCommand = mf.erasure.getConstructor(classOf[ActorSystem]).newInstance(system).asInstanceOf[T]
      newCommand.doBinding(params)
      request("_command_" + mf.erasure.getName) = newCommand
      newCommand
    }
  }
}
