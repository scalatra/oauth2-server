package org.scalatra
package oauth2
package json

import akka.actor.Uuid
import net.liftweb.json._

class AkkaUuidSerializer extends Serializer[Uuid] {

  val klass = classOf[Uuid]

  def deserialize(implicit format: Formats) = {
    case (TypeInfo(`klass`, _), json) if json == JNull ⇒ null.asInstanceOf[Uuid]
    case (TypeInfo(`klass`, _), json) ⇒ json match {
      case j: JString ⇒ new Uuid(j.s)
      case value      ⇒ throw new MappingException("Can't convert " + value + " to akka.actor.Uuid")
    }
  }

  def serialize(implicit format: Formats) = {
    case x: Uuid ⇒ JString(x.toString)
  }
}
