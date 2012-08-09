package org.scalatra
package oauth2
package service
package tests

import org.specs2._
import net.liftweb.json._
import JsonDSL._
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class JValuePathReaderSpec extends Specification {
  def is =
    "A JValuePathReader should" ^
      "read a top level property" ! readsTopLevel ^
      "read a second level property" ! readsSecondLevel ^
      "read a deeply nested property" ! readsDeeplyNested ^
      end

  implicit val formats: Formats = DefaultFormats
  val json: JValue =
    ("name" -> "Inbred Johnny") ~
      ("preferences" -> ("font" -> "comic sans")) ~
      ("addresses" ->
        ("shipping" ->
          ("home" ->
            ("first" ->
              ("street" -> "8577, hillbilly road") ~ ("city" -> "RedneckVille, TX")))))

  def readsTopLevel = {
    PathReading(json).read[String]("name") must beSome("Inbred Johnny")
  }

  def readsSecondLevel = {
    PathReading(json).read[String]("preferences.font") must beSome("comic sans")
  }

  def readsDeeplyNested = {
    PathReading(json).read[String]("addresses.shipping.home.first.street") must beSome("8577, hillbilly road")
  }
}