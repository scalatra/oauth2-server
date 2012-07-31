package org.scalatra
package oauth2

import model.{ ApiError, ApiErrorList, OAuth2Response, AuthSession }
import scalaz._
import net.liftweb.json._
import command.FieldError
import liftjson.LiftJsonSupport

trait Renderer[T] {
  def render(obj: T): JValue
}

trait LowPriorityRenderer { self: LiftJsonSupport ⇒
  implicit val product2Json: Renderer[Product] = new Renderer[Product] {
    def render(obj: Product): JValue = Extraction.decompose(obj)
  }
}
trait RendererSupport extends LowPriorityRenderer { self: LiftJsonSupport ⇒

  implicit def authSession2UserJson: Renderer[AuthSession]

  private val errorClass = classOf[FieldError]

  def renderValidation: RenderPipeline = {
    case Success(m: AuthSession) ⇒ OAuth2Response(renderObj(m))
    case Success(m: Product)     ⇒ OAuth2Response(renderObj(m))
    case Failure(f: FieldError)  ⇒ OAuth2Response(errors = ApiErrorList(List(ApiError(f.message))).toJValue)
    case Failure(fs: NonEmptyList[_]) if errorClass.isAssignableFrom(fs.head.getClass) ⇒ OAuth2Response(errors =
      ApiErrorList(fs.list.map(f ⇒ ApiError(f.asInstanceOf[FieldError].message))).toJValue)
  }

  private[this] def renderObj[T: Renderer](r: T) = implicitly[Renderer[T]].render(r)

}
