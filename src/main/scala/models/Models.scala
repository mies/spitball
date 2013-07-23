package models

import scala.language.reflectiveCalls

case class ErrorResponse(error: String, detail: Option[String] = None)
case class LogLine(line:String, time:java.util.Date)
case class LogValue(value:String, time:java.util.Date)
case class Measure(name:String, value:String, time:String)
