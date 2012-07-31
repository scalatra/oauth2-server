package org.scalatra
package oauth2

import command._
import model.{ BCryptPassword, Account, fieldNames }
import OAuth2Imports._
import util.conversion.TypeConverter
import _root_.scalaz._
import Scalaz._
import collection.generic.{ Shrinkable, Growable }
import net.liftweb.json._
import liftjson.{ LiftJsonSupport, LiftJsonSupportWithoutFormats }

package object commands {

  def get[C <: OAuth2Command](oauth: OAuth2Extension)(implicit mf: Manifest[C]): C = {
    val const = mf.erasure.getConstructor(classOf[OAuth2Extension])
    const.newInstance(oauth).asInstanceOf[C]
  }

  trait IsValidMethod { self: ValidationSupport ⇒
    def isValid = valid == Some(true)
  }

  trait ValueBinder[S] {
    def apply[T: Manifest](data: S, binding: Binding[T]): Binding[T]
  }

  class LiftJsonValueBinder(implicit formats: Formats) extends ValueBinder[JValue] {

    def apply[T: Manifest](data: JValue, binding: Binding[T]) = {
      val v = (data \ binding.name)
      v match {
        case JNothing   ⇒
        case JString(s) ⇒ binding(s)
        case jv         ⇒ binding(compact(render(jv)))
      }
      binding
    }
  }

  class ParamsBinder extends ValueBinder[MultiParams] {

    def apply[T: Manifest](multiParams: MultiParams, binding: Binding[T]) = {
      val mf = manifest[T]
      val retr = if (mf <:< manifest[Traversable[_]]) {
        multiParams.get(binding.name).map(_.mkString("[", ",", "]"))
      } else multiParams.get(binding.name).flatMap(_.headOption)
      retr foreach binding.apply
      binding
    }
  }

  class StringMapBinder extends ValueBinder[Map[String, String]] {

    def apply[T: Manifest](params: Map[String, String], binding: Binding[T]) = {
      params.get(binding.name) foreach binding.apply
      binding
    }
  }

  trait DefaultValueReaders { this: LiftJsonSupport ⇒

    implicit val jvalueBinder = new LiftJsonValueBinder
    implicit val multiParamsBinder = new ParamsBinder
    implicit val stringMapBinder = new StringMapBinder
  }

  trait ForceFromHeaders { self: Command ⇒

    def headersToForce: Set[String]
  }

  /* trait BindToSupport { this: Command ⇒
    private[this] var preBindingActions: List[BindingAction] = Nil

    private[this] var postBindingActions: List[BindingAction] = Nil
    /**
     * Add an action that will be evaluated before field binding occurs.
     */
    abstract override protected def beforeBinding(action: ⇒ Any) {
      preBindingActions = preBindingActions :+ (() ⇒ action)
    }

    /**
     * Add an action that will be evaluated after field binding has been done.
     */
    abstract override protected def afterBinding(action: ⇒ Any) {
      postBindingActions = postBindingActions :+ (() ⇒ action)
    }

    def bindTo[T : Manifest : ValueBinder](data: T, params: MultiParams = Map.empty, headers: Map[String, String] = Map.empty): this.type = {
      doBeforeBindingActions()
      bindings foreach { binding =>
        this match {
          case d: ForceFromParams if d.namesToForce.contains(binding.name) => bindFromParams(params, binding)
          case d: ForceFromHeaders if d.headersToForce.contains(binding.name) =>
            headers.get(binding.name) foreach binding.apply
          case _ => implicitly[ValueBinder[T]].apply(data, binding)
        }

      }
      doAfterBindingActions()
      this

    }

    private def bindFromParams(params: MultiParams, binding: Binding[_]) = {
      params.get(binding.name).flatMap(_.headOption) foreach binding.apply
    }

//    private def bindFromJson(data: JValue, binding: Binding[_]) = {
//      val d = (data \ binding.name)
//      d match {
//        case JNothing =>
//        case JString(s) => binding(s)
//        case jv => binding(compact(render(jv)))
//      }
//    }
//

    private def doBeforeBindingActions() {
      preBindingActions.foreach(_.apply())
    }

    private def doAfterBindingActions() {
      postBindingActions.foreach(_.apply())
    }

  }*/

  abstract class OAuth2Command(val oauth: OAuth2Extension) extends Command with ValidationSupport with CommandValidators with IsValidMethod with Growable[Command] with Shrinkable[Command] {

    implicit def toValidator[T: Zero](fn: T ⇒ FieldValidation[T]): Validator[T] = { case s ⇒ fn(~s) }

    implicit val stringToWebDate: TypeConverter[DateTime] = DateFormats.parse _

    implicit val stringToObjectId: TypeConverter[ObjectId] = safe(new ObjectId(_))

    implicit val stringToBCryptPassword: TypeConverter[BCryptPassword] = safeOption(BCryptPassword.parse)

    def +=(elem: Command) = {
      bindings :::= elem.bindings
      this
    }

    def clear() {
      bindings = List.empty[Binding[_]]
    }

    def -=(elem: Command) = {
      bindings = bindings filterNot (_ == elem)
      this
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