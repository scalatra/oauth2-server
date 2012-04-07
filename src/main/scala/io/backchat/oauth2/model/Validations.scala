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
import java.net.URI

sealed trait Error {
  def message: String
}
case class ValidationError(message: String, field: String) extends Error
case class SimpleError(message: String) extends Error
case class AlreadyConfirmed(message: String = "This account has already been confirmed.") extends Error
case class InvalidToken(message: String = "The token is invalid") extends Error
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

  def nonEmptyString(fieldName: String): Validator[String] =
    new PredicateValidator[String](fieldName, _.nonBlank, "%s must be present.")

  def nonEmptyCollection(fieldName: String): Validator[_ <: TraversableOnce[_]] =
    new PredicateValidator[TraversableOnce[_]](fieldName, _.nonEmpty, "%s must not be empty.")

  def validEmail(fieldName: String): Validator[String] =
    new PredicateValidator[String](fieldName, EmailValidator.getInstance.isValid(_), "%s must be a valid email.")

  def validUrl(fieldName: String, absolute: Boolean, schemes: String*): Validator[String] = {
    val urlValidator = if (schemes.isEmpty) UrlValidator.getInstance() else new UrlValidator(Array(schemes: _*))
    val validator = (s: String) ⇒ {
      urlValidator.isValid(s) && (!absolute || {
        try {
          URI.create(s).isAbsolute
        } catch { case _ ⇒ false }
      })
    }
    new PredicateValidator[String](fieldName, validator, "%s must be a valid url.")
  }

  def validFormat(fieldName: String, regex: Regex, messageFormat: String = "%s is invalid."): Validator[String] =
    new PredicateValidator[String](fieldName, regex.findFirstIn(_).isDefined, messageFormat)

  def validConfirmation(fieldName: String, confirmationFieldName: String, confirmationValue: String): Validator[String] =
    new PredicateValidator[String](fieldName, _ == confirmationValue, "%s must match " + confirmationFieldName.underscore.humanize.toLowerCase(ENGLISH) + ".")

  def greaterThan[T <% Ordered[T]](fieldName: String, min: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ > min, "%s must be greater than " + min.toString)

  def lessThan[T <% Ordered[T]](fieldName: String, max: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ < max, "%s must be less than " + max.toString)

  def greaterThanOrEqualTo[T <% Ordered[T]](fieldName: String, min: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ >= min, "%s must be greater than or equal to " + min.toString)

  def lessThanOrEqualTo[T <% Ordered[T]](fieldName: String, max: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ <= max, "%s must be less than or equal to " + max.toString)

  def minLength(fieldName: String, min: Int): Validator[String] =
    new PredicateValidator[String](fieldName, _.size >= min, "%s must be at least " + min.toString + " characters long.")

  def uniqueField[TResult](fieldName: String, collection: MongoCollection): Validator[TResult] = {
    new PredicateValidator[TResult](
      fieldName,
      s ⇒ collection.count(Map(fieldName -> s), Map(fieldName -> 1)) == 0,
      "%s exists already.")
  }

  def oneOf[TResult](fieldName: String, expected: TResult*): Validator[TResult] =
    new PredicateValidator[TResult](fieldName, expected.contains, "%s must be one of " + expected.mkString("[", ", ", "]"))

  def enumValue(fieldName: String, enum: Enumeration): Validator[String] =
    oneOf(fieldName, enum.values.map(_.toString).toSeq: _*)
}
