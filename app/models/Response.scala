package models

import net.liftweb.json._
import net.liftweb.json.JsonAST.JValue
import play.api.libs.json.Json
import play.api.mvc.{Controller, Result, Results}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class SuccessResponse(data: JValue, code: Int)

case class ErrorResponse(errors: JValue, code: Int)

case class CustomException(msg: Seq[String], status: Int) extends Exception

trait Response extends Controller {

  implicit val formats = DefaultFormats

  def response[T](data: Future[Either[CustomException, T]], status: Results.Status = Results.Ok): Future[Result] = {
    data.map {
      case Right(value) =>
        status(Json.parse(compactRender(Extraction.decompose(SuccessResponse(Extraction.decompose(value), status.header.status)))))
      case Left(cExp: CustomException) => status(Json.parse(compactRender(Extraction.decompose(ErrorResponse(Extraction.decompose(cExp.msg), cExp.status)))))
      case _ => InternalServerError(Json.parse(compactRender(Extraction.decompose(ErrorResponse(Extraction.decompose("Unexpecting exception.." :: Nil), 500)))))
    }
  }

}