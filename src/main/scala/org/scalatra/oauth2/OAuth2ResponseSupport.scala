package org.scalatra
package oauth2

import model.ApiErrorList
import model.OAuth2Response
import model.{ ApiErrorList, ApiError, OAuth2Response }
import org.json4s._
import OAuth2Imports._
import org.scalatra.{ ResponseStatus, ScalatraBase }
import _root_.scalaz._
import Scalaz._
import org.json4s.Xml._
import org.scalatra.json.JsonOutput

object OAuth2ResponseSupport {
  val JsonContentTypeHeader = Map("Content-Type" -> "application/json")
}
trait OAuth2ResponseSupport { self: ScalatraBase with ApiFormats with JsonOutput[_] ⇒

  import OAuth2ResponseSupport.JsonContentTypeHeader
  def halt(bcResponse: OAuth2Response): Nothing = {
    halt(500, bcResponse.copy(statusCode = Some(500)), JsonContentTypeHeader, "Bad request")
  }

  def halt(code: Int, bcResponse: OAuth2Response): Nothing = {
    halt(code, bcResponse.copy(statusCode = Some(code)), JsonContentTypeHeader)
  }

  def halt(error: JValue): Nothing = {
    halt(500, OAuth2Response(errors = JObject(JField("__global__", JArray(error :: Nil)) :: Nil), statusCode = Some(500)), JsonContentTypeHeader)
  }

  def halt(code: Int, error: JValue): Nothing = {
    halt(code, OAuth2Response(errors = JObject(JField("__global__", JArray(error :: Nil)) :: Nil), statusCode = Some(code)), JsonContentTypeHeader)
  }

  def halt(errors: List[JValue]): Nothing = {
    halt(500, OAuth2Response(errors = JObject(JField("__global__", JArray(errors)) :: Nil), statusCode = Some(500)), JsonContentTypeHeader)
  }

  def halt(code: Int, errors: List[JValue]): Nothing = {
    halt(code, OAuth2Response(errors = JObject(JField("__global__", JArray(errors)) :: Nil), statusCode = Some(code)), JsonContentTypeHeader)
  }

  def halt(code: Int, msg: String): Nothing = {
    halt(code, OAuth2Response(errors = JObject(JField("__global__", JArray(JObject(JField("message", JString(msg)) :: Nil) :: Nil)) :: Nil), statusCode = Some(code)), Map("Content-Type" -> "application/json"))
  }

  def halt(code: Int): Nothing = halt(code, OAuth2Response(statusCode = Some(code)), headers = Map("Content-Type" -> "application/json"))

  def halt(): Nothing = halt(500, OAuth2Response(statusCode = Some(500)), headers = Map("Content-Type" -> "application/json"))

  def includeStatusInResponse = {
    val g = params.get("statusInResponse") getOrElse ""
    g.asCheckboxBool
  }

  protected def renderOAuth2Response: PartialFunction[Any, Any] = {
    case x: JValue ⇒ OAuth2Response(data = x)
    case x: OAuth2Response ⇒ {
      response.characterEncoding = UTF_8.some
      x.statusCode foreach { sc ⇒
        response.status = ResponseStatus(sc)
      }
      val r = includeStatusInResponse ? x.copy(statusCode = x.statusCode orElse status.some) | x.copy(statusCode = None)
      val json = r.toJValue.snakizeKeys
      if (format == "xml") {
        contentType = "application/xml"
        writeJsonAsXml(json, response.writer)
      } else {
        writeJson(json, response.writer)
      }
      ()
    }
    case x: ApiError ⇒ ApiErrorList(x :: Nil)
    case x: ApiErrorList ⇒ {
      if (status < 400) status = 400
      OAuth2Response(errors = x.toJValue)
    }
  }

}