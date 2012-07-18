package org.scalatra
package oauth2

import akka.actor._
import model._
import net.liftweb.json.Formats
import service.SmtpTransport
import OAuth2Imports._
import collection.JavaConverters._
import org.scribe.builder.ServiceBuilder
import org.scribe.builder.api.{ Api, FacebookApi }
import com.typesafe.config.{ ConfigFactory, Config }
import com.mongodb.casbah.MongoURI

object OAuth2Extension extends ExtensionId[OAuth2Extension] with ExtensionIdProvider {
  def lookup() = OAuth2Extension

  def createExtension(system: ExtendedActorSystem) = new OAuth2Extension(system)

  val Production = "production"
  val Development = "development"
  val Staging = "staging"
  val Test = "test"

  private def readEnvironmentKey(failWith: String ⇒ Unit = _ ⇒ null) = {
    (envKey orElse propsKey) getOrElse {
      val inferred = "development"
      failWith("no environment found, defaulting to: " + inferred)
      inferred
    }
  }

  private def envKey = sys.env.get("SCALATRA_MODE") orElse sys.env.get("AKKA_MODE")
  private def propsKey = sys.props.get("scalatra.mode") orElse sys.props.get("akka.mode")

  val environment = readEnvironmentKey(System.err.println _)

  val isProduction = isEnvironment(Production)
  val isDevelopment = isEnvironment(Development)
  val isStaging = isEnvironment(Staging)
  val isTest = isEnvironment(Test)
  def isEnvironment(env: String) = environment equalsIgnoreCase env

}

case class OAuthProvider(name: String, clientId: String, clientSecret: String, scope: List[String] = Nil) {
  def service[SvcType <: Api: Manifest](urlFormat: String) = {
    val b = (new ServiceBuilder
      provider manifest[SvcType].erasure.asSubclass(classOf[Api])
      apiKey clientId
      apiSecret clientSecret
      callback urlFormat.format(name))
    if (scope.nonEmpty)
      b.scope(scope.mkString(","))
    b.build()
  }

}

class OAuth2Extension(system: ExtendedActorSystem) extends Extension {

  import OAuth2Extension.{ Production, Development, Staging, Test }

  def environment = OAuth2Extension.environment

  def isProduction = isEnvironment(Production)
  def isDevelopment = isEnvironment(Development)
  def isStaging = isEnvironment(Staging)
  def isTest = isEnvironment(Test)
  def isEnvironment(env: String) = environment equalsIgnoreCase env

  private[this] val cfg = {
    val c = system.settings.config
    val cc = if (c.hasPath(environment)) {
      c.getConfig(environment).withFallback(c)
    } else c
    cc.checkValid(ConfigFactory.defaultReference, "backchat")
    cc
  }
  private[this] def key(value: String) = confKey("mongo.%s" format value)

  val mongo = MongoConfiguration(cfg.getString(key("uri")))

  val defaultFormats: Formats = new OAuth2Formats

  lazy val userProvider = new AccountDao(mongo.db("resource_owners"))(system)
  lazy val clients = new ClientDao(mongo.db("clients"))(system)

  val smtp = new SmtpTransport(SmtpConfig(
    cfg.getString("scalatra.smtp.host"),
    cfg.getInt("scalatra.smtp.port"),
    MailSender(cfg.getString("scalatra.smtp.from.name"), cfg.getString("scalatra.smtp.from.email")),
    cfg.getString("scalatra.smtp.user").blankOption,
    cfg.getString("scalatra.smtp.password").blankOption,
    cfg.getBoolean("scalatra.smtp.sslRequired")))

  val web = WebConfig(
    cfg.getString(confKey("web.guiUrl")),
    cfg.getString(confKey("web.appUrl")),
    cfg.getString(confKey("web.domain")),
    cfg.getString(confKey("web.host")),
    cfg.getInt(confKey("web.port")),
    cfg.getBoolean(confKey("web.sslRequired")),
    cfg.getString(confKey("web.public")),
    AuthorizationType.withName(cfg.getString(confKey("web.authorizationType"))),
    cfg.getString(confKey("web.realm")),
    cfg.getBoolean(confKey("web.useParams")),
    CORSConfig(
      cfg.getStringList(confKey("web.cors.allowedOrigins")).asScala,
      cfg.getStringList(confKey("web.cors.allowedMethods")).asScala,
      cfg.getStringList(confKey("web.cors.allowedHeaders")).asScala,
      cfg.getBoolean(confKey("web.cors.allowCredentials")),
      cfg.getInt(confKey("web.cors.preflightMaxAge"))))

  private[this] val provPath = "scalatra.auth.providers"

  val permissions = {
    cfg.getConfigList(confKey("permissions")).asScala map { cc ⇒
      Permission(cc.getString("code"), cc.getString("name"), cc.getString("description"), true)
    }
  }
  val providers = new {
    def apply(key: String): OAuthProvider = {
      val v = cfg.getConfig(provPath + "." + key)
      val scope = v.getStringList("scope").asScala.toList
      OAuthProvider(key, v.getString("clientId"), v.getString("clientSecret"), scope)
    }
  }

}
