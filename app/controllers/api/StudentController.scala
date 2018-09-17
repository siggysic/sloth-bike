package controllers.api

import cats.implicits._
import com.typesafe.config.ConfigFactory
import javax.inject.Inject
import models._
import play.api.libs.json.{JsPath, JsSuccess, Reads}
import play.api.libs.functional.syntax._
import play.api.libs.ws
import play.api.libs.ws.{WSClient, WSResponse}
import repositories.StudentRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class StudentController @Inject()(studentRepository: StudentRepository, ws: WSClient) extends Response {

  import utils.Helpers.Authentication._
  import utils.Helpers.EitherHelper.CatchDatabaseExpAPI

  lazy val config = ConfigFactory.load()

  implicit val studentReads: Reads[Student] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "firstName").read[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "major").read[String] and
      (JsPath \ "profilePicture").read[String]
    )(Student.apply _)

  def getStudentsByStudentId(id: String) = authAsync { implicit request =>
    val result: Future[Either[CustomException, Student]] = (
      for {
        stu <- {
          Try {
            ws.url(s"${config.getString("student.api")}/$id").get().flatMap { ws =>
              ws.json.validate[Student] match {
                case JsSuccess(v, _) =>
                  studentRepository.create(v)
                  Future.successful(Right(v))
                case _ =>
                  studentRepository.getStudentById(id).map {
                    case Right(None) => Left(NotFoundThaiLangException("นักศึกษา"))
                    case Right(Some(stu)) => Right(stu)
                    case Left(other) => Left(other)
                  }
              }
            }
          }.getOrElse(
            studentRepository.getStudentById(id).map {
              case Right(None) => Left(NotFoundThaiLangException("นักศึกษา"))
              case Right(Some(stu)) => Right(stu)
              case Left(other) => Left(other)
            }
          ).expToEitherT
        }
      } yield stu
    ).value

    response(result)
  }

}
