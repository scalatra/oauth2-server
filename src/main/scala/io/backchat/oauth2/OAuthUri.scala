package io.backchat.oauth2

import OAuth2Imports._
import java.net.URI

class OAuthUri(uri: URI) {
  private var _query: Map[String, Seq[String]] = Map.empty
  def query = {
    if (_query == Map.empty[String, Seq[String]]) {
      if (uri.getQuery.nonBlank)
        _query ++= (if (uri.getQuery.indexOf('&') > -1) {
          uri.getQuery.split('&').foldRight(Map[String, List[String]]()) { readQsPair _ }
        } else {
          readQsPair(uri.getQuery)
        })
    }
    _query
  }

  private def readQsPair(pair: String, current: Map[String, List[String]] = Map.empty) = {
    (pair split '=' toList) map { s ⇒ if (s.nonBlank) s.urlDecode else "" } match {
      case item :: Nil ⇒ current + (item -> List[String]())
      case item :: rest ⇒
        if (!current.contains(item)) current + (item -> rest) else (current + (item -> (rest ::: current(item)).distinct))
      case _ ⇒ current
    }
  }

  def resources = parseResources(uri.getPath)

  private def parseResources(paths: String): Seq[String] = {
    val ps = if (paths.startsWith("/")) paths.substring(1) else paths
    if (ps.nonBlank) ps.split("/").map(_.urlDecode).toList else Nil
  }

}