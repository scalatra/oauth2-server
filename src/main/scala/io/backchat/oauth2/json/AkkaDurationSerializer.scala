package io.backchat.oauth2
package json

import akka.util.Duration
import java.util.concurrent.TimeUnit
import net.liftweb.json._

class AkkaDurationSerializer extends Serializer[Duration] {
  val klass = classOf[Duration]
  def deserialize(implicit format: Formats) = {
    case (TypeInfo(`klass`, _), json) if json == JNull ⇒ null.asInstanceOf[Duration]
    case (TypeInfo(`klass`, _), json) ⇒ json match {
      case JInt(j) ⇒ Duration(j.longValue, TimeUnit.MILLISECONDS)
      case value   ⇒ throw new MappingException("Can't convert " + value + " to akka.util.Duration")
    }
  }

  def serialize(implicit format: Formats) = {
    case d: Duration ⇒ JInt(d.toMillis)
  }
}
