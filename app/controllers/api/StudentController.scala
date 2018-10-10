package controllers.api

import cats.implicits._
import com.typesafe.config.ConfigFactory
import javax.inject.Inject
import models._
import play.api.libs.json._
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
      (JsPath \ "major").readNullable[String] and
      (JsPath \ "type").read[String] and
      (JsPath \ "status").read[String] and
      (JsPath \ "address").readNullable[String] and
      (JsPath \ "profilePicture").readNullable[String]
    )(Student.apply _)

  def getStudentsByStudentId(id: String) = authAsync { implicit request =>
    val result: Future[Either[CustomException, Student]] = (
      for {
        stu <- {
          studentRepository.getStudentById(id).flatMap {
            case Right(None) =>
              Try {
                ws.url(s"${config.getString("student.api")}/$id").get().map { ws =>
                  ws.json.validate[Student] match {
                    case JsSuccess(v, _) =>
                      studentRepository.create(v)
                      Right(v)
                    case _ =>
                      Left(NotFoundThaiLangException("นักศึกษา"))
                  }
                }
              }.getOrElse(Future.successful(Left(NotFoundThaiLangException("นักศึกษา"))))
            case Right(Some(stu)) => Future.successful(Right(stu))
            case Left(other) => Future.successful(Left(other))
          }.expToEitherT
        }
      } yield stu
    ).value

    response(result)
  }

}
