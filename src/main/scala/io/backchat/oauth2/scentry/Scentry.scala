package io.backchat.oauth2
package scentry

import org.scalatra._
import OAuth2Imports._
import collection.mutable.{ HashMap, Map ⇒ MMap }
import org.scalatra.ScalatraBase
import scala.PartialFunction
import ScentryAuthStore.{ SessionAuthStore, ScentryAuthStore }
import collection.immutable.List._

object Scentry {

  type StrategyFactory[UserType <: AnyRef] = ScalatraBase ⇒ ScentryStrategy[UserType]

  private val _globalStrategies = new HashMap[String, StrategyFactory[_ <: AnyRef]]()

  def registerStrategy[UserType <: AnyRef](name: String, strategyFactory: StrategyFactory[UserType]) =
    _globalStrategies += (name -> strategyFactory)

  def globalStrategies = _globalStrategies
  def clearGlobalStrategies() { _globalStrategies.clear() }

  val scentryAuthKey = "scentry.auth.default.user".intern()
  val ScentryRequestKey = "org.scalatra.auth.Scentry".intern
}

class Scentry[UserType <: AnyRef](
    app: ScalatraBase with SessionSupport,
    serialize: PartialFunction[UserType, String],
    deserialize: PartialFunction[String, UserType]) {

  type StrategyType = ScentryStrategy[UserType]
  type StrategyFactory = ScalatraBase ⇒ StrategyType

  import Scentry._
  private val _strategies = new HashMap[String, StrategyFactory]()
  private var _user: UserType = null.asInstanceOf[UserType]
  private var _store: ScentryAuthStore = new SessionAuthStore(app.session)

  @deprecated("use store_= instead", "2.0.0")
  def setStore(newStore: ScentryAuthStore) { store = newStore }
  def store = _store
  def store_=(newStore: ScentryAuthStore) {
    _store = newStore
  }

  def isAuthenticated = {
    userOption.isDefined
  }
  @deprecated("use isAuthenticated", "2.0.0")
  def authenticated_? = isAuthenticated

  //def session = app.session
  def params = app.params
  def redirect(uri: String) { app.redirect(uri) }

  def registerStrategy(name: String, strategyFactory: StrategyFactory) =
    _strategies += (name -> strategyFactory)

  def strategies: MMap[String, ScentryStrategy[UserType]] =
    (globalStrategies ++ _strategies) map { case (nm, fact) ⇒ (nm -> fact.asInstanceOf[StrategyFactory](app)) }

  def userOption: Option[UserType] = Option(user)

  def user: UserType = if (_user != null) _user else {
    val key = store.get
    if (key.nonBlank) {
      runCallbacks() { _.beforeFetch(key) }
      val res = fromSession(key)
      if (res != null) runCallbacks() { _.afterFetch(res) }
      _user = res
      res
    } else null.asInstanceOf[UserType]
  }

  def user_=(v: UserType) = {
    _user = v
    if (v != null) {
      runCallbacks() { _.beforeSetUser(v) }
      val res = toSession(v)
      store.set(res)
      runCallbacks() { _.afterSetUser(v) }
      res
    } else v
  }

  def fromSession = deserialize orElse missingDeserializer

  def toSession = serialize orElse missingSerializer

  private def missingSerializer: PartialFunction[UserType, String] = {
    case _ ⇒ throw new RuntimeException("You need to provide a session serializer for Scentry")
  }

  private def missingDeserializer: PartialFunction[String, UserType] = {
    case _ ⇒ throw new RuntimeException("You need to provide a session deserializer for Scentry")
  }

  def authenticate(names: String*) = {
    runAuthentication(names: _*) map {
      case (stratName, usr) ⇒
        runCallbacks() { _.afterAuthenticate(stratName, usr) }
        user = usr
        user
    } orElse { runUnauthenticated(names: _*) }
  }

  private def runAuthentication(names: String*) = {
    ((List[(String, UserType)]() /: strategies) {
      case (acc, (nm, strat)) ⇒
        val r = if (acc.isEmpty && strat.isValid && (names.isEmpty || names.contains(nm))) {
          runCallbacks(_.isValid) { _.beforeAuthenticate }
          strat.authenticate() match {
            case Some(usr) ⇒ (nm, usr) :: Nil
            case _         ⇒ List.empty[(String, UserType)]
          }
        } else List.empty[(String, UserType)]
        acc ::: r
    }).headOption
  }

  private def runUnauthenticated(names: String*) = {
    (strategies filter { case (name, strat) ⇒ strat.isValid && (names.isEmpty || names.contains(name)) }).values.toList match {
      case Nil ⇒ defaultUnauthenticated foreach { _.apply() }
      case l ⇒ {
        l foreach { s ⇒ runCallbacks() { _.unauthenticated() } }
        //        if (names.isEmpty) defaultUnauthenticated foreach { _.apply() }
      }
    }
    None

  }

  private var defaultUnauthenticated: Option[() ⇒ Unit] = None

  def unauthenticated(callback: ⇒ Unit) {
    defaultUnauthenticated = Some(() ⇒ callback)
  }

  def logout() {
    val usr = user.asInstanceOf[UserType]
    runCallbacks() { _.beforeLogout(usr) }
    if (_user != null) _user = null.asInstanceOf[UserType]
    store.invalidate
    runCallbacks() { _.afterLogout(usr) }
  }

  private def runCallbacks(guard: StrategyType ⇒ Boolean = s ⇒ true)(which: StrategyType ⇒ Unit) {
    strategies foreach {
      case (_, v) if guard(v) ⇒ which(v)
      case _                  ⇒ // guard failed
    }
  }
}

