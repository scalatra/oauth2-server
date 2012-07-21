package org.scalatra
package oauth2
package model

import scalaz._
import Scalaz._
import com.novus.salat.global._
import OAuth2Imports._
import net.liftweb.json._
import org.apache.commons.validator.routines.{ UrlValidator, EmailValidator }
import scala.util.matching.Regex
import scala.util.control.Exception._
import java.net.{ HttpURLConnection, URI }
import command.Validation.PredicateValidator
import command.FieldValidation

object Validations {

  def uniqueField[TResult](fieldName: String, value: ⇒ TResult, collection: MongoCollection, currentItem: Option[ObjectId] = None): FieldValidation[TResult] = {
    def q(s: TResult) = Map(fieldName -> s)
    def newQ(s: TResult) = currentItem.fold(id ⇒ q(s) ++ Map("_id" -> ("$ne" -> id)), q(s))
    new PredicateValidator[TResult](
      fieldName,
      s ⇒ collection.count(newQ(s), Map(fieldName -> 1)) == 0,
      "%s exists already.").validate(value)
  }
}
