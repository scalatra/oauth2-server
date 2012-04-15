package io.backchat.oauth2.model.tests

import org.specs2.time.NoTimeConversions
import org.specs2.Specification
import org.specs2.matcher.{ Expectable, Matcher }
import scalaz._
import Scalaz._

/**
 * Utility methods to replace a null String with "null"
 *
 * They also make sure that the toString or mkString methods don't throw exceptions when being evaluated
 */
private[tests] trait NotNullStrings {

  /**
   * try to evaluate an expression, returning a value T
   *
   * If the expression throws an Exception a function f is used to return a value
   * of the expected type.
   */
  def tryOr[T](a: ⇒ T)(implicit f: Exception ⇒ T): T = {
    trye(a)(f).fold(identity, identity)
  }

  /**
   * try to evaluate an expression, returning Either
   *
   * If the expression throws an Exception a function f is used to return the left value
   * of the Either returned value.
   */
  def trye[T, S](a: ⇒ T)(implicit f: Exception ⇒ S): Either[S, T] = {
    try { Right(a) }
    catch { case e: Exception ⇒ Left(f(e)) }
  }

  implicit def anyToNotNull(a: Any) = new NotNullAny(a)
  class NotNullAny(a: Any) {
    def notNull: String = {
      if (a == null) "null"
      else {
        a match {
          case ar: Array[_]           ⇒ ar.notNullMkString(", ", "Array(", ")")
          case it: TraversableOnce[_] ⇒ it.notNullMkString(", ")
          case _                      ⇒ evaluate(a, "Exception when evaluating toString ")
        }
      }
    }
  }

  private def evaluate(value: ⇒ Any, msg: String) = {
    val string = tryOr(value.toString) { (e: Exception) ⇒ msg + e.getMessage }
    if (string == null) "null"
    else string
  }

  trait NotNullMkString {
    def notNullMkString(sep: String, start: String = "", end: String = ""): String
  }
  implicit def arrayToNotNull[T](a: Array[T]): NotNullMkString = if (a == null) new NullMkString else new NotNullTraversableOnce(a.toSeq)

  class NullMkString extends NotNullMkString {
    def notNullMkString(sep: String, start: String = "", end: String = ""): String = "null"
  }

  implicit def traversableOnceToNotNull[T](a: ⇒ TraversableOnce[T]): NotNullTraversableOnce[T] = new NotNullTraversableOnce(a)
  class NotNullTraversableOnce[T](a: ⇒ TraversableOnce[T]) extends NotNullMkString {
    def notNullMkString(sep: String, start: String = "", end: String = ""): String = {
      if (a == null) "null"
      else evaluate(a.mkString(start, sep, end), "Exception when evaluating mkString ")
    }
  }

}
private[tests] object NotNullStrings extends NotNullStrings

class BeSuccessMatcher extends Matcher[Validation[_, _]] {
  //  def apply[S <: Validation[_, _]](v: Expectable[S]) = {
  //    Matcher.result(v.value.isSuccess, v.description + " is success", v.description + " is failure", v)
  //  }
  def apply[S <: Validation[_, _]](v: Expectable[S]) = {
    Matcher.result(v.value.isSuccess, v.description + " is success", v.description + " is failure", v)
  }
}

trait OAuth2ServerSpecification extends Specification with NoTimeConversions with NotNullStrings {

  private[this] def q(a: Any) = "'" + a.notNull + "'"
  def beSuccess[T] = new BeSuccessMatcher

  def beSuccess[T](t: ⇒ T) = new Matcher[Validation[_, T]] {
    def apply[S <: Validation[_, T]](value: Expectable[S]) = {
      val expected = t
      result(value.value == Success(expected),
        value.description + " is Success with value " + q(expected),
        value.description + " is not Success with value " + q(expected),
        value)
    }
  }

  def beFailure[T] = beSuccess[T].not
}