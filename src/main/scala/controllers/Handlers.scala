package controllers

import spray.routing.{HttpService, RejectionHandler, ExceptionHandler, MalformedRequestContentRejection}
import spray.httpx.SprayJsonSupport._
import spray.http.HttpBody
import spray.http.ContentType.`application/json`
import spray.http.StatusCodes._
import spray.json._
import models.ErrorResponse
import Formatters._

trait Handlers {
  this: HttpService =>

  private val asJsonError = mapHttpResponseEntity {
    case b: HttpBody => HttpBody(`application/json`, ErrorResponse(b.asString).toJson.toString())
    case e           => e
  }

  implicit val rejectionHandler: RejectionHandler = RejectionHandler.fromPF {
    case MalformedRequestContentRejection(msg) :: _ =>
      complete(BadRequest, ErrorResponse(msg))
    case r if (RejectionHandler.Default.isDefinedAt(r)) =>
      asJsonError(RejectionHandler.Default.apply(r))
  }

  implicit val exceptionHandler = ExceptionHandler.fromPF {
    case e if ExceptionHandler.default.isDefinedAt(e) =>
      asJsonError(ExceptionHandler.default.apply(e))
  }
}
