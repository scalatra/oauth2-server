package io.backchat.oauth2
package model

import collection.mutable.ListBuffer
import net.liftweb.json._

object ApiError {
  def apply(message: String): ApiError = ApiError(None, message)
  def apply(fieldName: String, message: String): ApiError = ApiError(Some(fieldName), message)
}
case class ApiError(fieldName: Option[String], message: String) {

  def toJValue = {
    val lb = new ListBuffer[JValue]
    fieldName foreach { lb += JString(_) }
    lb += JString(message)
    JArray(lb.toList)
  }
}

case class ApiErrorList(values: List[ApiError]) {
  def toJValue = {
    values.map(_.toJValue)
  }
}

case class OAuth2Response(data: JValue = JNull, errors: List[JValue] = Nil, statusCode: Option[Int] = None) {
  def toJValue = {
    val obj = JObject(JField("data", data) :: JField("errors", JArray(errors)) :: Nil)
    statusCode map { sc ⇒ obj merge JObject(JField("statusCode", JInt(sc)) :: Nil) } getOrElse obj
  }
}
