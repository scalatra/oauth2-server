package org.scalatra
package oauth2

import model._
import scalaz._
import Scalaz._
import net.liftweb.json._
import JsonDSL._
import command.{ SimpleError, ValidationError, FieldError }
import liftjson.LiftJsonSupport
import akka.actor.ActorSystem
import OAuth2Imports._

trait Renderer[T] {
  def render(obj: T): JValue
}

trait LowPriorityRenderer { self: LiftJsonSupport ⇒
  implicit val product2Json: Renderer[Product] = new Renderer[Product] {
    def render(obj: Product): JValue = Extraction.decompose(obj)
  }
}
trait RendererSupport extends LowPriorityRenderer { self: LiftJsonSupport ⇒

  private val errorClass = classOf[FieldError]

  private def errorsWithPriority: PartialFunction[FieldError, (Int, FieldError)] = {
    case m: LoginFailed         ⇒ (1, m)
    case m: NotFound            ⇒ (2, m)
    case m: InvalidToken        ⇒ (3, m)
    case m: NotUnique           ⇒ (4, m)
    case m: AlreadyConfirmed    ⇒ (5, m)
    case m: ValidationError     ⇒ (6, m)
    case m: BadGatewayError     ⇒ (7, m)
    case m: ServiceUnavailable  ⇒ (8, m)
    case m: GatewayTimeout      ⇒ (9, m)
    case m: ServerError         ⇒ (10, m)
    case m: NotImplementedError ⇒ (11, m)
    case m                      ⇒ (99, m)
  }

  def renderValidation: RenderPipeline = {
    case Success(m: Product)    ⇒ OAuth2Response(renderObj(m))
    case Failure(f: FieldError) ⇒ Failure(nel(f))
    case Failure(fs: NonEmptyList[_]) if errorClass.isAssignableFrom(fs.head.getClass) ⇒
      fieldErrorListAsActionResult(fs.map(_.asInstanceOf[FieldError]).list)
  }

  protected def renderObj[T: Renderer](r: T) = implicitly[Renderer[T]].render(r)

  protected def fieldErrorListAsActionResult(ferrs: List[FieldError]): ActionResult = {
    // Pick the error to determine the status code
    val err = (ferrs map errorsWithPriority.apply).sortWith(_._1 < _._1).headOption.map(_._2) getOrElse ServerError()
    fieldError2ActionResult(err)(oauthResponseFor(ferrs))
  }
  protected def fieldError2ActionResult: PartialFunction[FieldError, OAuth2Response ⇒ ActionResult] = {
    case m: NotUnique           ⇒ (o: OAuth2Response) ⇒ Conflict(o)
    case m: ValidationError     ⇒ (o: OAuth2Response) ⇒ UnprocessableEntity(o)
    case m: ServerError         ⇒ (o: OAuth2Response) ⇒ InternalServerError(o)
    case m: LoginFailed         ⇒ (o: OAuth2Response) ⇒ Unauthorized(o)
    case m: NotFound            ⇒ (o: OAuth2Response) ⇒ org.scalatra.NotFound(o)
    case m: InvalidToken        ⇒ (o: OAuth2Response) ⇒ org.scalatra.NotFound(o)
    case m: AlreadyConfirmed    ⇒ (o: OAuth2Response) ⇒ Conflict(o)
    case m: BadGatewayError     ⇒ (o: OAuth2Response) ⇒ BadGateway(o)
    case m: NotImplementedError ⇒ (o: OAuth2Response) ⇒ NotImplemented(o)
    case m: ServiceUnavailable  ⇒ (o: OAuth2Response) ⇒ org.scalatra.ServiceUnavailable(o)
    case m: GatewayTimeout      ⇒ (o: OAuth2Response) ⇒ org.scalatra.GatewayTimeout(o)
    case m                      ⇒ (o: OAuth2Response) ⇒ BadRequest(o)
  }

  private def oauthResponseFor(fieldErrors: List[FieldError]) =
    OAuth2Response(errors = JArray(fieldErrors map fieldError2JValue.apply))

  protected def fieldError2JValue: PartialFunction[FieldError, JValue] = {
    case m: NotUnique       ⇒ ("message" -> m.message) ~ ("fieldName" -> m.field)
    case m: ValidationError ⇒ ("message" -> m.message) ~ ("fieldName" -> m.field)
    case m ⇒
      val ms: JValue = ("message" -> m.message)
      if (m.args.nonEmpty) ms.merge(("args" -> m.args.map(Extraction.decompose(_))): JValue) else ms
  }

}

trait OAuth2RendererSupport extends RendererSupport { self: LiftJsonSupport ⇒
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
  def render(obj: AuthSession): _root_.net.liftweb.json.JValue = {
    val u = obj.account
    ("login" -> u.login) ~ ("email" -> u.email) ~ ("name" -> u.name) ~ ("createdAt" -> u.createdAt) ~ ("updatedAt" -> u.updatedAt)
  }
}

class AccountRenderer(implicit system: ActorSystem) extends Renderer[Account] {
  implicit val formats: Formats = new OAuth2Formats
  def render(obj: Account): _root_.net.liftweb.json.JValue = {
    val u = obj
    ("login" -> u.login) ~ ("email" -> u.email) ~ ("name" -> u.name) ~ ("createdAt" -> u.createdAt) ~ ("updatedAt" -> u.updatedAt)
  }
}
