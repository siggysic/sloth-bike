package models

import net.liftweb.json._
import net.liftweb.json.JsonAST.JValue
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Controller, Result, Results}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class SuccessResponse(data: JValue, code: Int)

case class ErrorResponse(errors: JValue, code: Int)

abstract class CustomException(msg: Seq[String], status: Int) {
  def getMsg: Seq[String] = msg

  def getStatus: Int = status
}

case class GenericException(msg: Seq[String], status: Int) extends CustomException(msg, status)

case class NotFoundException(topic: String) extends CustomException(s"$topic not found".trim :: Nil, 400)

case object UnauthorizedException extends CustomException("Token is invalid" :: Nil, 401)

case object DBException extends CustomException("Database exception" :: Nil, 400)

trait Response extends Controller {

  implicit val formats = DefaultFormats

  def response[T](data: Future[Either[CustomException, T]], status: Results.Status = Results.Ok): Future[Result] = {
    data.map {
      case Right(value) =>
        status(Json.parse(compactRender(Extraction.decompose(SuccessResponse(Extraction.decompose(value), status.header.status)))))
      case Left(cExp: CustomException) => responseStatus(Json.parse(compactRender(Extraction.decompose(ErrorResponse(Extraction.decompose(cExp.getMsg), cExp.getStatus)))), cExp.getStatus)
      case _ => InternalServerError(Json.parse(compactRender(Extraction.decompose(ErrorResponse(Extraction.decompose("Unexpecting exception.." :: Nil), 500)))))
    }
  }

  private def responseStatus(jsValue: JsValue, status: Int): Result = status match {
    case 200 => Ok(jsValue)
    case 201 => Created(jsValue)
    case 400 => BadRequest(jsValue)
    case 401 => Unauthorized(jsValue)
    case 404 => NotFound(jsValue)
    case _ => InternalServerError(jsValue)
  }

}