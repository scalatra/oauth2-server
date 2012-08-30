package org.scalatra
package oauth2
package service

import org.json4s._
import OAuth2Imports._

object PathReading {

  trait Separator {
    def beginning: String
    def end: String
  }
  object Separator {
    object Dotted extends Separator {
      val beginning = "."
      val end = ""
    }
    object Dollar extends Separator {
      val beginning = "$"
      val end = ""
    }
    object Slash extends Separator {
      val beginning = "/"
      val end = ""
    }
    object BackSlash extends Separator {
      val beginning = "$"
      val end = ""
    }
    object Bracketed extends Separator {
      val beginning = "["
      val end = "]"
    }
  }
  val DefaultSeparator = Separator.Dotted

  abstract class PathReader[Subject: Manifest] {
    def separator: Separator
    def obj: Subject

    protected def get[TResult](key: String, subj: Subject)(implicit mf: Manifest[TResult]): Option[TResult]

    def apply[TResult: Manifest](path: String, subj: Subject = obj): TResult =
      read[TResult](path, subj).get

    final def read[TResult: Manifest](path: String, subj: Subject = obj): Option[TResult] = {
      val partIndex = path.indexOf(separator.beginning)
      val (part, rest) = if (path.indexOf(separator.beginning) > -1) path.splitAt(partIndex) else (path, "")
      val realRest = if (rest.nonEmpty) {
        if (separator.end.nonBlank) {
          if (rest.size > 1) rest.substring(2) else rest.substring(1)
        } else rest.substring(1)
      } else rest
      if (realRest.isEmpty) {
        get[TResult](part, subj)
      } else {
        get[Subject](part, subj) flatMap (read[TResult](realRest, _))
      }
    }

  }

  class JValuePathReader(val obj: JValue, val separator: Separator = DefaultSeparator)(implicit formats: Formats, subjMf: Manifest[JValue]) extends PathReader[JValue] {
    protected def get[TResult](key: String, subj: JValue)(implicit mf: Manifest[TResult]) = {
      if (key.contains(separator)) read[TResult](key, subj) else {
        val jv = (subj \ key)
        if (subjMf <:< mf)
          jv match {
            case JNull | JNothing ⇒ None
            case _                ⇒ Some(jv.asInstanceOf[TResult])
          }
        else
          jv.extractOpt[TResult]
      }
    }
  }

  def apply(subj: JValue)(implicit formats: Formats): JValuePathReader = new JValuePathReader(subj)
}