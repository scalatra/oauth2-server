package org.scalatra
package oauth2
package model

import com.novus.salat.global._
import OAuth2Imports._
import command.{ Validators, ValidationError, FieldValidation }
import command.Validators.{ Validator, PredicateValidator }
import scalaz._
import Scalaz._

object Validations {

  def uniqueField[TResult](fieldName: String, value: ⇒ TResult, collection: MongoCollection, currentItem: Option[ObjectId] = None): FieldValidation[TResult] = {
    def q(s: TResult) = Map(fieldName -> s)
    def newQ(s: TResult) = currentItem.fold(id ⇒ q(s) ++ Map("_id" -> ("$ne" -> id)), q(s))
    new Validator[TResult] {
      def validate[R >: TResult <: TResult](subject: R): FieldValidation[R] = {
        if (collection.count(newQ(subject), Map(fieldName -> 1)) == 0) subject.success
        else NotUnique(fieldName.humanize + " exists already.", fieldName).fail
      }
    }.validate(value)
  }
}
