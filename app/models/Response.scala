package models

case class SuccessResponse(data: Json, code: Int, status: String)

case class ErrorResponse(error: Json, code: Int, status: String)