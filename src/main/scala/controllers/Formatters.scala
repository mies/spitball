package controllers

import spray.json._
import java.net.URL
import models._


object Formatters extends DefaultJsonProtocol {

  implicit val errorResponseFormat = jsonFormat2(ErrorResponse)

  implicit val measureFormatter = jsonFormat3(Measure)

  implicit val urlFormats = new RootJsonFormat[URL] {
    def write(c: URL) = JsString(c.toString)

    def read(value: JsValue) = value match {
      case JsString(url) => new URL(url)
      case _             => throw new DeserializationException("Expecting URL to be a JsString")
    }
  }
}
