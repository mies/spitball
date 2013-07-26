package models

import scala.language.reflectiveCalls

case class ErrorResponse(error: String, detail: Option[String] = None)

case class LogLine(time: Long,line: String)

case class Measure(name: String, value: String, time: Long)
