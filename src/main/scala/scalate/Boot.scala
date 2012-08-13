package scalate

import org.fusesource.scalate.TemplateEngine
import java.io.File
import org.fusesource.scalate.util.Logging
import org.fusesource.scalate.Binding

class Boot(engine: TemplateEngine) extends Logging {

  def run: Unit = {
    engine.templateDirectories ++= Seq("src/main/webapp/WEB-INF")
    engine.importStatements ++= Seq(
      "import scalaz._",
      "import Scalaz._",
      "import org.scalatra.oauth2._",
      "import OAuth2Imports._",
      "import model._")
    engine.bindings ++= Seq(
      Binding("flash", "scala.collection.Map[String, Any]", defaultValue = Some("Map.empty")),
      Binding("session", "javax.servlet.http.HttpSession"),
      Binding("sessionOption", "scala.Option[javax.servlet.http.HttpSession]"),
      Binding("params", "scala.collection.Map[String, String]"),
      Binding("multiParams", "org.scalatra.MultiParams"),
      Binding("userOption", "Option[AuthSession]", defaultValue = Some("None")),
      Binding("user", "AuthSession", defaultValue = Some("null")),
      Binding("system", "akka.actor.ActorSystem", isImplicit = true),
      Binding("isAnonymous", "Boolean", defaultValue = Some("true")),
      Binding("isAuthenticated", "Boolean", defaultValue = Some("false")))

  }
}