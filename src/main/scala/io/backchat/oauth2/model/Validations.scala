package io.backchat.oauth2
package model

import scalaz._
import Scalaz._
import com.novus.salat.global._
import OAuth2Imports._
import net.liftweb.json._
import org.apache.commons.validator.routines.{ UrlValidator, EmailValidator }
import util.matching.Regex
import model.Validations.PredicateValidator
import util.control.Exception._
import java.net.{ HttpURLConnection, URI }

object Validations {

  trait Validator[TValue] {
    def validate[TResult >: TValue <: TValue](subject: TResult): Validation[Error, TResult]
  }

  class PredicateValidator[TValue](fieldName: String, isValid: TValue ⇒ Boolean, messageFormat: String)
      extends Validator[TValue] {
    override def validate[TResult >: TValue <: TValue](value: TResult): Validation[Error, TResult] = {
      if (isValid(value)) value.success
      else ValidationError(messageFormat.format(fieldName.humanize), fieldName.underscore).fail[TResult]
    }
  }

  def nonEmptyString(fieldName: String, value: ⇒ String): Validation[Error, String] =
    new PredicateValidator[String](fieldName, _.nonBlank, "%s must be present.").validate(value)

  def nonEmptyCollection[TResult <: Seq[_]](fieldName: String, value: ⇒ TResult): Validation[Error, TResult] =
    new PredicateValidator[TResult](fieldName, _.nonEmpty, "%s must not be empty.").validate(value)

  def validEmail(fieldName: String, value: ⇒ String): Validation[Error, String] =
    new PredicateValidator[String](fieldName, EmailValidator.getInstance.isValid(_), "%s must be a valid email.").validate(value)

  def validAbsoluteUrl(fieldName: String, value: ⇒ String, schemes: String*) =
    buildUrlValidator(fieldName, value, true, schemes: _*)

  def validUrl(fieldName: String, value: ⇒ String, schemes: String*) =
    buildUrlValidator(fieldName, value, false, schemes: _*)

  private def buildUrlValidator(fieldName: String, value: ⇒ String, absolute: Boolean, schemes: String*): Validation[Error, String] = {
    val validator = (url: String) ⇒ {
      (allCatch opt {
        val u = URI.create(url).normalize()
        !absolute || u.isAbsolute
      }).isDefined && (!OAuth2Extension.isProduction || UrlValidator.getInstance().isValid(url))
    }
    new PredicateValidator[String](fieldName, validator, "%s must be a valid url.").validate(value)
  }

  def validFormat(fieldName: String, value: ⇒ String, regex: Regex, messageFormat: String = "%s is invalid."): Validation[Error, String] =
    new PredicateValidator[String](fieldName, regex.findFirstIn(_).isDefined, messageFormat).validate(value)

  def validConfirmation(fieldName: String, value: ⇒ String, confirmationFieldName: String, confirmationValue: String): Validation[Error, String] =
    new PredicateValidator[String](
      fieldName,
      _ == confirmationValue,
      "%s must match " + confirmationFieldName.underscore.humanize.toLowerCase(ENGLISH) + ".").validate(value)

  def greaterThan[T <% Ordered[T]](fieldName: String, value: ⇒ T, min: T): Validation[Error, T] =
    new PredicateValidator[T](fieldName, _ > min, "%s must be greater than " + min.toString).validate(value)

  def lessThan[T <% Ordered[T]](fieldName: String, value: ⇒ T, max: T): Validation[Error, T] =
    new PredicateValidator[T](fieldName, _ < max, "%s must be less than " + max.toString).validate(value)

  def greaterThanOrEqualTo[T <% Ordered[T]](fieldName: String, value: ⇒ T, min: T): Validation[Error, T] =
    new PredicateValidator[T](fieldName, _ >= min, "%s must be greater than or equal to " + min.toString).validate(value)

  def lessThanOrEqualTo[T <% Ordered[T]](fieldName: String, value: ⇒ T, max: T): Validation[Error, T] =
    new PredicateValidator[T](fieldName, _ <= max, "%s must be less than or equal to " + max.toString).validate(value)

  def minLength(fieldName: String, value: ⇒ String, min: Int): Validation[Error, String] =
    new PredicateValidator[String](
      fieldName, _.size >= min, "%s must be at least " + min.toString + " characters long.").validate(value)

  def uniqueField[TResult](fieldName: String, value: ⇒ TResult, collection: MongoCollection, currentItem: Option[ObjectId] = None): Validation[Error, TResult] = {
    def q(s: TResult) = Map(fieldName -> s)
    def newQ(s: TResult) = currentItem.fold(id ⇒ q(s) ++ Map("_id" -> ("$ne" -> id)), q(s))
    new PredicateValidator[TResult](
      fieldName,
      s ⇒ collection.count(newQ(s), Map(fieldName -> 1)) == 0,
      "%s exists already.").validate(value)
  }

  def oneOf[TResult](fieldName: String, value: ⇒ TResult, expected: TResult*): Validation[Error, TResult] =
    new PredicateValidator[TResult](
      fieldName, expected.contains, "%s must be one of " + expected.mkString("[", ", ", "]")).validate(value)

  def enumValue(fieldName: String, value: ⇒ String, enum: Enumeration): Validation[Error, String] =
    oneOf(fieldName, value, enum.values.map(_.toString).toSeq: _*)
}
