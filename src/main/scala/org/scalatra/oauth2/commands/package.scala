package org.scalatra
package oauth2

import command._
import OAuth2Imports._
import util.conversion.TypeConverter
import scalaz._
import Scalaz._

package object commands {

  def get[C <: OAuth2Command](oauth: OAuth2Extension)(implicit mf: Manifest[OAuth2Command]): C = {
    val const = mf.erasure.getConstructor(classOf[OAuth2Extension])
    const.newInstance(oauth).asInstanceOf[C]
  }

  abstract class OAuth2Command(val oauth: OAuth2Extension) extends Command with ValidationSupport with CommandValidators {

    implicit def toValidator[T: Zero](fn: T ⇒ FieldValidation[T]): Validator[T] = {
      case s ⇒ fn(~s)
    }

    implicit val stringToWebDate: TypeConverter[DateTime] = DateFormats.parse _

    implicit val stringToObjectId: TypeConverter[ObjectId] = safe(new ObjectId(_))
  }
}