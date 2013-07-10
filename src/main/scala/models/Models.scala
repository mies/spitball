package models

import scala.language.reflectiveCalls

case class ErrorResponse(error: String, detail: Option[String] = None)