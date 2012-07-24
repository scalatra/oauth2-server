package org.scalatra
package oauth2
package model

import net.liftweb.json._
import collection.mutable

object ApiError {
  def apply(message: String): ApiError = ApiError(None, message)
  def apply(fieldName: String, message: String): ApiError = ApiError(Some(fieldName), message)
}
case class ApiError(fieldName: Option[String], message: String) {

  def toJValue: JValue = {
    val buff = new mutable.ListBuffer[JField]
    fieldName foreach { fn ⇒ buff += JField("fieldName", JString(fn)) }
    buff += JField("message", JString(message))
    JObject(buff.toList)
  }
}

case class ApiErrorList(values: List[ApiError]) {
  def toJValue = {
    //    val (fieldErrors, globalErrors) = values.partition(_.fieldName.isDefined)
    //    val groupedByField = (fieldErrors.groupBy(_.fieldName.get) map {
    //      case (nm, e) ⇒ nm -> e.map(_.toJValue)
    //    })
    //    val all = groupedByField + ("__global__" -> globalErrors.map(_.toJValue))
    //    JObject((all map {
    //      case (nm, vals) ⇒ JField(nm, JArray(vals))
    //    }).toList)
    JArray(values.map(_.toJValue))
  }
}

case class OAuth2Response(data: JValue = JNull, errors: JValue = JArray(Nil), statusCode: Option[Int] = None) {
  def toJValue: JValue = {
    val obj = JObject(JField("data", data) :: JField("errors", errors) :: Nil)
    statusCode map { sc ⇒ obj merge JObject(JField("statusCode", JInt(sc)) :: Nil) } getOrElse obj
  }
}
