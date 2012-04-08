package io.backchat.oauth2

import model.{ ApiErrorList, ApiError, OAuth2Response }
import net.liftweb.json._
import OAuth2Imports._
import org.scalatra.{ ResponseStatus, ScalatraBase }
import scalaz._
import Scalaz._

trait OAuth2ResponseSupport { self: ScalatraBase ⇒

  def halt(bcResponse: OAuth2Response): Nothing = {
    halt(500, bcResponse.copy(statusCode = Some(500)), Map("Content-Type" -> "application/json"), "Bad request")
  }

  def halt(code: Int, bcResponse: OAuth2Response): Nothing = {
    halt(code, bcResponse.copy(statusCode = Some(code)), Map("Content-Type" -> "application/json"))
  }

  def halt(error: JValue): Nothing = {
    halt(500, OAuth2Response(errors = error :: Nil, statusCode = Some(500)), Map("Content-Type" -> "application/json"))
  }

  def halt(code: Int, error: JValue): Nothing = {
    halt(code, OAuth2Response(errors = error :: Nil, statusCode = Some(code)), Map("Content-Type" -> "application/json"))
  }

  def halt(errors: List[JValue]): Nothing = {
    halt(500, OAuth2Response(errors = errors, statusCode = Some(500)), Map("Content-Type" -> "application/json"))
  }

  def halt(code: Int, errors: List[JValue]): Nothing = {
    halt(code, OAuth2Response(errors = errors, statusCode = Some(code)), Map("Content-Type" -> "application/json"))
  }

  def halt(code: Int, msg: String): Nothing = {
    halt(code, OAuth2Response(errors = JArray(JString("") :: JString(msg) :: Nil) :: Nil, statusCode = Some(code)), Map("Content-Type" -> "application/json"))
  }

  def halt(code: Int): Nothing = halt(code, OAuth2Response(statusCode = Some(code)), headers = Map("Content-Type" -> "application/json"))

  def halt(): Nothing = halt(500, OAuth2Response(statusCode = Some(500)), headers = Map("Content-Type" -> "application/json"))

  def includeStatusInResponse = {
    val g = params.get("statusInResponse") getOrElse ""
    g.asCheckboxBool
  }

  protected def renderBackchatResponse: PartialFunction[Any, Any] = {
    case x: JValue ⇒ OAuth2Response(data = x)
    case x: OAuth2Response ⇒ {
      x.statusCode foreach { sc ⇒
        response.status = ResponseStatus(sc)
      }
      val r = includeStatusInResponse ? x.copy(statusCode = x.statusCode orElse status.some) | x.copy(statusCode = None)
      Printer.compact(render(r.toJValue.snakizeKeys), response.writer)
    }
    case x: ApiError ⇒ ApiErrorList(x :: Nil)
    case x: ApiErrorList ⇒ {
      if (status < 400) status = 400
      OAuth2Response(errors = x.toJValue)
    }
  }

}