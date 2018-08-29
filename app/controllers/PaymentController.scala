package controllers

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import models.{FullPayment, PaymentQuery}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import repositories.{PaymentRepository, StudentRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentController @Inject()(paymentRepository: PaymentRepository, studentRepository: StudentRepository)(implicit assets: AssetsFinder) extends Controller {


  def getPayments
  (studentId: Option[String] = None, firstName: Option[String] = None, lastName: Option[String] = None, major: Option[String] , page: Int = 1, pageSize: Int = 10) =
    Action.async { implicit request: Request[AnyContent] =>

      val queryStudentId = studentId.getOrElse("")
      val queryFirstName = firstName.getOrElse("")
      val queryLastName = lastName.getOrElse("")
      val queryMajor = major.getOrElse("")

      val action = for {
        majors <- {
          val results = studentRepository.getMajors
          EitherT(results)
        }

        payments <- {
          val results = paymentRepository.getFullPayment(queryStudentId, queryFirstName, queryLastName, queryMajor, page, pageSize)
          EitherT(results)
        }
      } yield {
        (payments._1.map(record => FullPayment(record._1.getOrElse(""), record._2, record._3,
          record._4, record._5, record._6, record._7, record._8)), payments._2, majors )
      }

      val currentForm = queryForm.fill(PaymentQuery(studentId, firstName, lastName, major, page, pageSize))

      action.value.map {
        case Right(ok) => Ok(views.html.payments(ok._1, ok._2, ok._3, currentForm))
        case Left(_) => InternalServerError("DB Error")
      }
  }

  val queryForm: Form[PaymentQuery] = Form(
    mapping(
      "studentId" -> optional(text),
      "firstName" -> optional(text),
      "lastName" -> optional(text),
      "major" -> optional(text),
      "page" -> default(number(min = 1), 1),
      "pageSize" -> default(number, 10)
    )(PaymentQuery.apply)(PaymentQuery.unapply)
  )

  def filterPaymentAction = Action { implicit request: Request[AnyContent] =>

    val failureFn = { formWithError: Form[PaymentQuery] =>
      val studentId = formWithError.data.get("studentId")
      val firstName = formWithError.data.get("firstName")
      val lastName = formWithError.data.get("lastName")
      val major = formWithError.data.get("major")
      val page = formWithError.data.get("page").map(_.toInt).getOrElse(1)
      Redirect(routes.PaymentController.getPayments(studentId, firstName, lastName, major, page))
    }

    val successFn = { query: PaymentQuery =>
      Redirect(routes.PaymentController.getPayments(query.studentId, query.firstName, query.lastName, query.major, query.page))
    }

    queryForm.bindFromRequest().fold(failureFn, successFn)

  }
}