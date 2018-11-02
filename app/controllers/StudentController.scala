package controllers

import java.io.{File, PrintWriter}
import java.nio.file.Paths

import cats.data.EitherT
import javax.inject.{Inject, Singleton}
import models._
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import repositories.{StationRepository, StudentRepository}
import utils.Helpers.EitherHelper

import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.Files.TemporaryFile.temporaryFileToPath

import scala.concurrent.{ExecutionContext, Future}
import utils.Helpers.EitherHelper.{CatchDatabaseExp, ExtractEitherT}

@Singleton()
class StudentController @Inject()(studentRepository: StudentRepository, stationRepository: StationRepository, cc: ControllerComponents)(implicit assets: AssetsFinder, ec: ExecutionContext)
  extends AbstractController(cc) {

  val queryForm: Form[StationQuery] = Form(
    mapping(
      "name" -> optional(text),
      "availableAsset" -> optional(text),
      "page" -> default(number(min = 1), 1),
      "pageSize" -> default(number, 10)
    )(StationQuery.apply)(StationQuery.unapply)
  )

  val userForm: Form[Student] = Form(
    mapping(
      "id" -> text.verifying(Contraints.validateText),
      "firstName" -> text.verifying(Contraints.validateText),
      "lastName" -> text.verifying(Contraints.validateText),
      "phone" -> text.verifying(Contraints.validateText),
      "major" -> optional(text),
      "type" -> text.verifying(Contraints.validateText),
      "status" -> text.verifying(Contraints.validateText),
      "address" -> optional(text),
      "department" -> optional(text),
      "profilePicture" -> optional(text)
    )(Student.apply)(Student.unapply)
  )

  val studentQueryForm: Form[StudentQuery] = Form(
    mapping(
      "id" -> optional(text),
      "name" -> optional(text),
      "type" -> optional(text),
      "page" -> default(number(min = 1), 1),
      "pageSize" -> default(number, 10)
    )(StudentQuery.apply)(StudentQuery.unapply)
  )

  val fields = StudentFields()

  def viewUsers
  (id: Option[String], name: Option[String] = None, `type`: Option[String] = None, page: Int = 1, pageSize: Int =10) = Action.async { implicit request: Request[AnyContent] =>

    val actualPage = page match {
      case p if p >= 1 => p
      case _ => 1
    }

    val currentForm = studentQueryForm.fill(StudentQuery(id, name, `type`, actualPage, pageSize))

    val resp = for {
      respUsers <- {
        val r = studentRepository.getUsers(StudentQuery(id, name, `type`, actualPage, pageSize))
        EitherT(r)
      }
      typs <- {
        val r = studentRepository.getType
        EitherT(r)
      }
    } yield {
      val (total, users) = respUsers
      (users, total, typs)
    }

    resp.value map {
      case Right(r) =>
        Ok(views.html.viewUsers(r._1, r._3, r._2, currentForm))
      case Left(_) =>
        BadRequest(views.html.exception("Database exception."))
    }
  }

  def filterStudentAction = Action { implicit request: Request[AnyContent] =>

    val failureFn = { formWithError: Form[StationQuery] =>
      val id = formWithError.data.get("id")
      val name = formWithError.data.get("name")
      val typ = formWithError.data.get("type")
      val page = formWithError.data.get("page").map(_.toInt).getOrElse(1)
      val pageSize = formWithError.data.get("pageSize").map(_.toInt).getOrElse(10)
      Redirect(routes.StudentController.viewUsers(
        id, name, typ, page, pageSize))
    }

    val successFn = { query: StationQuery =>
      Redirect(routes.StationController.viewStations(query.name, query.availableAsset))
    }

    queryForm.bindFromRequest().fold(failureFn, successFn)

  }

  def viewInsertUser = Action.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.usersInsert(userForm, fields)))
  }

  def viewEditUser(id: String) = Action.async { implicit request: Request[AnyContent] =>
    (
      for(
        student <- studentRepository.getStudentById(id).dbExpToEitherT
      )yield student match {
        case Some(stu) => Ok(views.html.usersEdit(userForm.fillAndValidate(stu), fields))
        case None => BadRequest(views.html.exception("Database exception."))
      }
    ).extract
  }

  def insertUser = Action.async { implicit request: Request[AnyContent] =>

    val reqMul = request.map(s => s.asMultipartFormData)

    val errorFunction = { formWithFailure: Form[Student] =>
      Future.successful(BadRequest(views.html.usersInsert(formWithFailure, fields)))
    }

    val successFunction = { student: Student =>
      (
        for {
          result <- {
            val resp = reqMul.body match {
              case Some(file) if file.file("profilePicture").map(_.filename != "").forall(identity) => file.file("profilePicture").map {
                case filePart @ FilePart(key, filename, contentType, file) =>
                  studentRepository.getStudentById(student.id).flatMap {
                    case Right(Some(_)) =>
                      Future.successful(Left(BadRequest(views.html.usersInsert(userForm.fillAndValidate(student)
                        .withError(FormError("id", "ID is already exists." :: Nil)), fields))))
                    case Right(None) if filename != "" =>
                      val newStudent = student.copy(profilePicture = Some(s"${new java.util.Date().getTime}-$filename"))
                      file.moveTo(Paths.get(s"public/images/users/${newStudent.profilePicture.get}"), replace = true)
                      studentRepository.create(newStudent).map {
                        case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                        case _ => Left(BadRequest(views.html.exception("Database exception.")))
                      }
                    case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
                  }

                case _ => Future.successful(Left(BadRequest(views.html.exception("File upload exception."))))
              }.getOrElse(
                Future.successful(Left(BadRequest(views.html.exception("File upload exception."))))
              )
              case _ => studentRepository.getStudentById(student.id).flatMap {
                case Right(Some(_)) =>
                  Future.successful(Left(BadRequest(views.html.usersInsert(userForm.fillAndValidate(student)
                    .withError(FormError("id", "ID is already exists." :: Nil)), fields))))
                case Right(None) =>
                  studentRepository.create(student).map {
                    case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                    case _ => Left(BadRequest(views.html.exception("Database exception.")))
                  }
                case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
              }
            }
            EitherT(resp)
          }
        } yield result
      ).extract
    }

    val formValidate: Form[Student] = userForm.bindFromRequest
    formValidate.fold(
      errorFunction,
      successFunction
    )
  }

  def editUser(id: String) = Action.async { implicit request: Request[AnyContent] =>

    val reqMul = request.map(s => s.asMultipartFormData)

    val errorFunction = { formWithFailure: Form[Student] =>
      Future.successful(BadRequest(views.html.usersEdit(formWithFailure, fields)))
    }

    val successFunction = { student: Student =>
      (
        for {
          result <- {
            val resp = reqMul.body match {
              case Some(file) => file.file("profilePicture").map {
                case filePart @ FilePart(key, filename, contentType, file) =>
                  studentRepository.getStudentById(id).flatMap {
                    case Right(Some(_)) =>
                      studentRepository.getStudentById(student.id).flatMap {
                        case Right(Some(stu)) if id == student.id =>
                          if(filename == "") {
                            val newStudent = student.copy(profilePicture = Some(s"${new java.util.Date().getTime}-$filename"))
                            studentRepository.create(newStudent.copy(profilePicture = stu.profilePicture)).map {
                              case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                              case _ => Left(BadRequest(views.html.exception("Database exception.")))
                            }
                          } else {
                            val newStudent = student.copy(profilePicture = Some(s"${new java.util.Date().getTime}-$filename"))
                            file.moveTo(Paths.get(s"public/images/users/${newStudent.profilePicture.get}"), replace = true)
                            studentRepository.create(newStudent).map {
                              case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                              case _ => Left(BadRequest(views.html.exception("Database exception.")))
                            }
                          }
                        case Right(None) =>
                          val newStudent = student.copy(profilePicture = Some(s"${new java.util.Date().getTime}-$filename"))
                          file.moveTo(Paths.get(s"public/images/users/${newStudent.profilePicture.get}"), replace = true)
                          studentRepository.create(newStudent).map {
                            case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                            case _ => Left(BadRequest(views.html.exception("Database exception.")))
                          }
                        case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
                      }
                    case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
                  }

                case _ => Future.successful(Left(BadRequest(views.html.exception("File upload exception."))))
              }.getOrElse(
                Future.successful(Left(BadRequest(views.html.exception("File upload exception."))))
              )
              case None => studentRepository.getStudentById(student.id).flatMap {
                case Right(Some(_)) =>
                  studentRepository.getStudentById(student.id).flatMap {
                    case Right(Some(_)) if id == student.id =>
                      studentRepository.create(student).map {
                        case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                        case _ => Left(BadRequest(views.html.exception("Database exception.")))
                      }
                    case Right(None) =>
                      studentRepository.create(student).map {
                        case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                        case _ => Left(BadRequest(views.html.exception("Database exception.")))
                      }
                    case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
                  }
                case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
              }
            }
            EitherT(resp)
          }
        } yield result
        ).extract
    }

    val formValidate: Form[Student] = userForm.bindFromRequest
    formValidate.fold(
      errorFunction,
      successFunction
    )
  }

}
