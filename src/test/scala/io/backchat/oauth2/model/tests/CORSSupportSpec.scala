package io.backchat.oauth2
package model
package tests

import org.scalatra.test.specs2.ScalatraSpec
import org.scalatra.ScalatraServlet

class CORSSupportSpec extends AkkaSpecification with ScalatraSpec {

  addServlet(new ScalatraServlet with CORSSupport {
    override protected lazy val corsConfig =
      CORSConfig(List("http://www.example.com"), List("GET", "HEAD", "POST"), "X-Requested-With,Authorization,Content-Type,Accept,Origin".split(",").toSeq, true)

    get("/") {
      "OK"
    }

    implicit def system = CORSSupportSpec.this.system
  }, "/*")

  def is =
    "The CORS support should" ^
      "augment a valid simple request" ! context.validSimpleRequest ^
      "not touch a regular request" ! context.dontTouchRegularRequest ^
      "respond to a valid preflight request" ! context.validPreflightRequest ^
      "respond to a valid preflight request with headers" ! context.validPreflightRequestWithHeaders ^ end


  object context {
    def validSimpleRequest = {
      get("/", headers = Map(CORSSupport.ORIGIN_HEADER -> "http://www.example.com")) {
        response.getHeader(CORSSupport.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER) must_== "http://www.example.com"
      }
    }
    def dontTouchRegularRequest = {
      get("/") {
        response.getHeader(CORSSupport.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER) must beNull
      }
    }

    def validPreflightRequest = {
      options("/", headers = Map(CORSSupport.ORIGIN_HEADER -> "http://www.example.com", CORSSupport.ACCESS_CONTROL_REQUEST_METHOD_HEADER -> "GET", "Content-Type" -> "application/json")) {
        response.getHeader(CORSSupport.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER) must_== "http://www.example.com"
      }
    }

    def validPreflightRequestWithHeaders = {
      val hdrs = Map(
        CORSSupport.ORIGIN_HEADER -> "http://www.example.com",
        CORSSupport.ACCESS_CONTROL_REQUEST_METHOD_HEADER -> "GET",
        CORSSupport.ACCESS_CONTROL_REQUEST_HEADERS_HEADER -> "Origin, Authorization, Accept",
        "Content-Type" -> "application/json")
      options("/", headers = hdrs) {
        response.getHeader(CORSSupport.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER) must_== "http://www.example.com"
      }
    }
  }
}