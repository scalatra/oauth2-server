package org.scalatra
package oauth2

import _root_.org.scalatra.databinding._
import _root_.org.scalatra.validation._
import model.{ BCryptPassword, Account, fieldNames }
import OAuth2Imports._
import util.conversion.{ TypeConverterSupport, TypeConverter }
import _root_.scalaz._
import Scalaz._
import collection.generic.{ Shrinkable, Growable }
import org.json4s._

package object commands {

  def get[C <: OAuth2Command[_]](oauth: OAuth2Extension, args: Any*)(implicit mf: Manifest[C], formats: Formats): C = {
    val argClasses = Seq[Class[_]](classOf[OAuth2Extension]) ++ args.map(_.getClass)
    val allArgs = Seq[AnyRef](oauth) ++ args
    val const = mf.erasure.getConstructor(argClasses: _*)
    const.newInstance(allArgs.map(_.asInstanceOf[AnyRef]): _*).asInstanceOf[C]
  }

  abstract class OAuth2Command[S](val oauth: OAuth2Extension)(implicit mf: Manifest[S], protected val jsonFormats: Formats) extends JsonCommand {

    type Result = S
    //    implicit def toValidator[T: Zero](fn: T ⇒ FieldValidation[T]): Validator[T] = { case s ⇒ fn(~s) }

    //    implicit val stringToWebDate: TypeConverter[String, DateTime] = DateFormats.parse _

    //    implicit val stringToObjectId: TypeConverter[String, ObjectId] = safe(new ObjectId(_))

    implicit val bcryptTypeConverterFactory = new JsonTypeConverterFactory[BCryptPassword] {
      protected implicit val jsonFormats: Formats = OAuth2Command.this.jsonFormats
      implicit val stringToBCryptPassword: TypeConverter[String, BCryptPassword] = safeOption(BCryptPassword.parse)
      implicit val jsonToBCryptPassword: TypeConverter[JValue, BCryptPassword] =
        safeOption(_.extractOpt[String] flatMap BCryptPassword.parse)
      implicit val stringSeqHeadToBCryptPassword: TypeConverter[Seq[String], BCryptPassword] =
        seqHead(stringToBCryptPassword)
      implicit val stringSeqToBCryptPasswordSeq: TypeConverter[Seq[String], Seq[BCryptPassword]] =
        seqToSeq(stringToBCryptPassword)

      def resolveJson: TypeConverter[JValue, BCryptPassword] = implicitly[TypeConverter[JValue, BCryptPassword]]

      def resolveMultiParams: TypeConverter[Seq[String], BCryptPassword] = implicitly[TypeConverter[Seq[String], BCryptPassword]]

      def resolveStringParams: TypeConverter[String, BCryptPassword] = implicitly[TypeConverter[String, BCryptPassword]]

    }

  }

  trait OAuth2CommandPart {
    val oauth: OAuth2Extension
  }

  //  trait IdCommand[ObjectType, ID <: Any] { self: OAuth2Command ⇒
  //
  //    lazy val retrieved: FieldValidation[Account] = {
  //      val r = id.converted.flatMap(oauth.userProvider.findOneById(_))
  //      r some (_.success) none FieldError("Not found").fail[]
  //    }
  //
  //    def id: ValidatedBinding[ID]
  //
  //  }

}