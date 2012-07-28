package org.scalatra
package oauth2

import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }
import scala.util.control.Exception._
import OAuth2Imports._

trait DateParser {
  def parse(s: String): Option[DateTime]
  def unapply(s: String) = parse(s)
}

object DateFormats extends DateParser {
  val All = DateFormats(Iso8601NoMillis, Iso8601, HttpDate)

  def parse(s: String) = All.parse(s)

  def apply(f: DateFormat*) = new DateParser {
    def parse(s: String) = f.toList.foldLeft (None: Option[DateTime]) { (r, f) ⇒ if (!r.isDefined) f.parse(s) else r }
  }

  trait DateFormat extends DateParser {
    def dateTimeformat: DateTimeFormatter

    def parse(s: String) = Option(s) flatMap { s ⇒
      catching(classOf[IllegalArgumentException]) opt {
        dateTimeformat parseDateTime s
      }
    }
  }

  object Iso8601 extends DateFormat {
    val dateTimeformat = Iso8601Date
  }

  object Iso8601NoMillis extends DateFormat {
    val dateTimeformat = Iso8601DateNoMillis
  }

  object HttpDate extends DateFormat {
    val dateTimeformat = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss zzz")
  }
}