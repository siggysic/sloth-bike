package utils

import net.liftweb.json.{DefaultFormats, JObject, JValue}
import play.api.libs.json.JsValue

import scala.util.Try

object Helpers {
  object JsonHelper {
    implicit val format = DefaultFormats

    implicit class ExtendJsValue(json: JsValue) {
      def toJValue: JValue = Try(net.liftweb.json.parse(json.toString())).getOrElse(JObject(Nil))
    }
  }
}
