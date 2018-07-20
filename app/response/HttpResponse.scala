package response

import play.api.libs.json.JsValue


case class SuccessResponse(data: JsValue, statusCode: Int)

case class ErrorResponse(error: String, statusCode: Int)
