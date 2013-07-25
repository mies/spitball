package models

import scala.language.reflectiveCalls

case class ErrorResponse(error: String, detail: Option[String] = None)

case class LogLine(line: String, time: Long)

case class Measure(name: String, value: String, time: Long)
