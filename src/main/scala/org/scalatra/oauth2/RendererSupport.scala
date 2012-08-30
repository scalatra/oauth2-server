package org.scalatra
package oauth2

import model._
import scalaz._
import Scalaz._
import org.json4s._
import JsonDSL._
import akka.actor.ActorSystem
import OAuth2Imports._
import org.scalatra.json.NativeJsonSupport
import org.scalatra.validation._

trait Renderer[T] {
  def render(obj: T): JValue
}

trait LowPriorityRenderer { self: NativeJsonSupport ⇒
  protected implicit def jsonFormats: Formats
  implicit val product2Json: Renderer[Product] = new Renderer[Product] {
    def render(obj: Product): JValue = Extraction.decompose(obj)
  }
}
trait RendererSupport extends LowPriorityRenderer { self: NativeJsonSupport ⇒

  private val errorClass = classOf[ValidationError]

  private def errorsWithPriority: PartialFunction[ValidationError, (Int, ValidationError)] = {
    case m @ ValidationError(_, _, Some(LoginFailed), _) ⇒ (1, m)
    case m @ ValidationError(_, _, Some(NotFound), _) ⇒ (2, m)
    case m @ ValidationError(_, _, Some(InvalidToken), _) ⇒ (3, m)
    case m @ ValidationError(_, _, Some(NotUnique), _) ⇒ (4, m)
    case m @ ValidationError(_, _, Some(AlreadyConfirmed), _) ⇒ (5, m)
    case m @ ValidationError(_, Some(_), None, _) ⇒ (6, m)
    case m @ ValidationError(_, _, Some(ValidationFail), _) ⇒ (6, m)
    case m @ ValidationError(_, _, Some(BadGateway), _) ⇒ (7, m)
    case m @ ValidationError(_, _, Some(ServiceUnavailable), _) ⇒ (8, m)
    case m @ ValidationError(_, _, Some(GatewayTimeout), _) ⇒ (9, m)
    case m @ ValidationError(_, _, Some(UnknownError), _) ⇒ (10, m)
    case m @ ValidationError(_, _, Some(NotImplemented), _) ⇒ (11, m)
    case m ⇒ (99, m)
  }

  def renderValidation: RenderPipeline = {
    case Success(m: Product)         ⇒ OAuth2Response(renderObj(m))
    case Failure(f: ValidationError) ⇒ Failure(nel(f))
    case Failure(fs: NonEmptyList[_]) if errorClass.isAssignableFrom(fs.head.getClass) ⇒
      fieldErrorListAsActionResult(fs.map(_.asInstanceOf[ValidationError]).list)
  }

  protected def renderObj[T: Renderer](r: T) = implicitly[Renderer[T]].render(r)

  protected def fieldErrorListAsActionResult(ferrs: List[ValidationError]): ActionResult = {
    // Pick the error to determine the status code
    val err = (ferrs map errorsWithPriority.apply).sortWith(_._1 < _._1).headOption.map(_._2) getOrElse OAuth2Error.ServerError
    fieldError2ActionResult(err)(oauthResponseFor(ferrs))
  }
  protected def fieldError2ActionResult: PartialFunction[ValidationError, OAuth2Response ⇒ ActionResult] = {
    case m @ ValidationError(_, _, Some(NotUnique), _) ⇒ (o: OAuth2Response) ⇒ Conflict(o)
    case m @ ValidationError(_, Some(_), None, _) ⇒ (o: OAuth2Response) ⇒ UnprocessableEntity(o)
    case m @ ValidationError(_, _, Some(ValidationFail), _) ⇒ (o: OAuth2Response) ⇒ UnprocessableEntity(o)
    case m @ ValidationError(_, _, Some(UnknownError), _) ⇒ (o: OAuth2Response) ⇒ InternalServerError(o)
    case m @ ValidationError(_, _, Some(LoginFailed), _) ⇒ (o: OAuth2Response) ⇒ Unauthorized(o)
    case m @ ValidationError(_, _, Some(NotFound), _) ⇒ (o: OAuth2Response) ⇒ org.scalatra.NotFound(o)
    case m @ ValidationError(_, _, Some(InvalidToken), _) ⇒ (o: OAuth2Response) ⇒ org.scalatra.NotFound(o)
    case m @ ValidationError(_, _, Some(AlreadyConfirmed), _) ⇒ (o: OAuth2Response) ⇒ Conflict(o)
    case m @ ValidationError(_, _, Some(BadGateway), _) ⇒ (o: OAuth2Response) ⇒ org.scalatra.BadGateway(o)
    case m @ ValidationError(_, _, Some(NotImplemented), _) ⇒ (o: OAuth2Response) ⇒ org.scalatra.NotImplemented(o)
    case m @ ValidationError(_, _, Some(ServiceUnavailable), _) ⇒ (o: OAuth2Response) ⇒ org.scalatra.ServiceUnavailable(o)
    case m @ ValidationError(_, _, Some(GatewayTimeout), _) ⇒ (o: OAuth2Response) ⇒ org.scalatra.GatewayTimeout(o)
    case m ⇒ (o: OAuth2Response) ⇒ BadRequest(o)
  }

  private def oauthResponseFor(fieldErrors: List[ValidationError]) =
    OAuth2Response(errors = JArray(fieldErrors map fieldError2JValue.apply))

  protected def fieldError2JValue: PartialFunction[ValidationError, JValue] = {
    //    case m: NotUnique       ⇒ ("message" -> m.message) ~ ("fieldName" -> m.field)
    case m ⇒
      val ms: JValue = ("message" -> m.message)
      val mf = if (m.field.nonEmpty) ms.merge(("fieldName" -> m.field.map(_.name)): JValue) else ms
      if (m.args.nonEmpty) mf.merge(("args" -> m.args.map(Extraction.decompose(_))): JValue) else mf
  }

}

trait OAuth2RendererSupport extends RendererSupport { self: NativeJsonSupport ⇒
  protected implicit def system: ActorSystem
  implicit def authSession2UserJson: Renderer[AuthSession] = new AuthSessionRenderer
  implicit def account2UserJson: Renderer[Account] = new AccountRenderer

  override def renderValidation: RenderPipeline = ({
    case Success(m: AuthSession) ⇒ OAuth2Response(renderObj(m))
    case Success(m: Account)     ⇒ OAuth2Response(renderObj(m))
  }: RenderPipeline) orElse super.renderValidation

}

class AuthSessionRenderer(implicit system: ActorSystem) extends Renderer[AuthSession] {
  implicit val formats: Formats = new OAuth2Formats
  def render(obj: AuthSession): JValue = {
    val u = obj.account
    ("login" -> u.login) ~ ("email" -> u.email) ~ ("name" -> u.name) ~ ("createdAt" -> u.createdAt) ~ ("updatedAt" -> u.updatedAt)
  }
}

class AccountRenderer(implicit system: ActorSystem) extends Renderer[Account] {
  implicit val formats: Formats = new OAuth2Formats
  def render(obj: Account): JValue = {
    val u = obj
    ("login" -> u.login) ~ ("email" -> u.email) ~ ("name" -> u.name) ~ ("createdAt" -> u.createdAt) ~ ("updatedAt" -> u.updatedAt)
  }
}
