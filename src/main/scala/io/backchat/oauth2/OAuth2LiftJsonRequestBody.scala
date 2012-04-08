package io.backchat.oauth2

import net.liftweb.json._
import net.liftweb.json.Xml.toJson
import java.io.InputStreamReader
import org.scalatra.{ApiFormats, MatchedRoute, ScalatraBase}

trait OAuth2LiftJsonRequestBody extends ScalatraBase with ApiFormats {

  protected implicit val jsonFormats: Formats = new OAuth2Formats

  import OAuth2LiftJsonRequestBody._

  protected def parseRequestBody(format: String) = try {
    if (format == "json") {
      transformRequestBody(JsonParser.parse(new InputStreamReader(request.inputStream)))
    } else if (format == "xml") {
      transformRequestBody(toJson(scala.xml.XML.load(request.inputStream)))
    } else JNothing
  } catch {
    case _ ⇒ JNothing
  }

  protected def transformRequestBody(body: JValue) = body

  override protected def invoke(matchedRoute: MatchedRoute) = {
    withRouteMultiParams(Some(matchedRoute)) {
      val mt = request.contentType map {
        _.split(";").head
      } getOrElse "application/x-www-form-urlencoded"
      val fmt = mimeTypes get mt getOrElse "html"
      if (shouldParseBody(fmt)) {
        request(ParsedBodyKey) = parseRequestBody(fmt)
      }
      super.invoke(matchedRoute)
    }
  }

  private def shouldParseBody(fmt: String) =
    (fmt == "json" || fmt == "xml") && parsedBody != JNothing

  def parsedBody = request.get(ParsedBodyKey) getOrElse JNothing

}

object OAuth2LiftJsonRequestBody {

  val ParsedBodyKey = "org.scalatra.liftjson.ParsedBody".intern
}
