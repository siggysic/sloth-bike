package controllers.api

import cats.implicits._
import javax.inject.Inject
import models._
import repositories.StudentRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StudentController @Inject()(studentRepository: StudentRepository) extends Response {

  import utils.Helpers.Authentication._
  import utils.Helpers.EitherHelper.CatchDatabaseExpAPI

  def getStudentsByStudentId(id: String) = authAsync { implicit request =>
    val result: Future[Either[CustomException, Student]] = (
      for {
        st <- studentRepository.getStudentById(id).map {
          case Right(None) => Left(NotFoundException("Student"))
          case Right(Some(stu)) => Right(stu)
          case Left(other) => Left(other)
        }.expToEitherT
      } yield st
    ).value

    response(result)
  }

}
