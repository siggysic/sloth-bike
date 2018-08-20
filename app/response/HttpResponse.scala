package response

import net.liftweb.json.DefaultFormats
import play.api.libs.json.{JsValue, Json}
import net.liftweb.json.Serialization.write

case class SuccessResponse(data: JsValue, statusCode: Int)

case class ErrorResponse(error: String, statusCode: Int) {
  implicit val formats = DefaultFormats

  def toJsValue = {
    Json.parse(write(this))
  }
}
