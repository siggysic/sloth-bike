package controllers

import java.io.{File, FileOutputStream}

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import models.{FullPayment, PaymentQuery}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import repositories.{PaymentRepository, StudentRepository}
import utils.Helpers.EitherHelper.ExtractEitherT

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class PaymentController @Inject()(cc: ControllerComponents, paymentRepository: PaymentRepository, studentRepository: StudentRepository)(implicit assets: AssetsFinder) extends AbstractController(cc) {


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
          record._4, record._5, record._6.getOrElse(""), record._7, record._8)), payments._2, majors )
      }

      val currentForm = queryForm.fill(PaymentQuery(studentId, firstName, lastName, major, page, pageSize))

      action.value.map {
        case Right(ok) => Ok(views.html.payments(ok._1, ok._2, ok._3.flatten, currentForm))
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

  def filterPaymentAction = Action.async { implicit request: Request[AnyContent] =>

    val form: Form[PaymentQuery] = queryForm.bindFromRequest


    val failureFn = { formWithError: Form[PaymentQuery] =>
      val studentId = formWithError.data.get("studentId")
      val firstName = formWithError.data.get("firstName")
      val lastName = formWithError.data.get("lastName")
      val major = formWithError.data.get("major")
      val page = formWithError.data.get("page").map(_.toInt).getOrElse(1)
      Future.successful(Redirect(routes.PaymentController.getPayments(studentId, firstName, lastName, major, page)))
    }

    val successFn = { query: PaymentQuery =>
      form("submit-action").value match {
        case Some("search") =>
          Future.successful(Redirect(routes.PaymentController.getPayments(query.studentId, query.firstName, query.lastName, query.major, query.page)))
        case Some("export") =>
          exportPayment(query)
        case _ =>
          Future.successful(Redirect(routes.PaymentController.getPayments(query.studentId, query.firstName, query.lastName, query.major, query.page)))
      }
    }

    queryForm.bindFromRequest().fold(failureFn, successFn)

  }

  def exportPayment(req: PaymentQuery)(implicit request: Request[AnyContent]) = {

      val columns = List("รหัสนักศึกษา", "ชื่อ-สกุล", "คณะ", "ค่าปรับเกินเวลา(บาท)", "ค่าปรับชำรุด(บาท)", "รวมค่าปรับ(บาท)")

      val queryStudentId = req.studentId.getOrElse("")
      val queryFirstName = req.firstName.getOrElse("")
      val queryLastName = req.lastName.getOrElse("")
      val queryMajor = req.major.getOrElse("")

      (for {
        payments <- {
          val results = paymentRepository.getAllFullPayment(queryStudentId, queryFirstName, queryLastName, queryMajor) map {
            case Right(data) => Right(data)
            case Left(_) => Left(InternalServerError(views.html.exception("Database exception.")))
          }
          EitherT(results)
        }
      } yield {
        val searchResult: Seq[FullPayment] = payments.map(record => FullPayment(record._1.getOrElse(""), record._2, record._3,
          record._4, record._5, record._6.getOrElse(""), record._7, record._8))
        val workbook = new XSSFWorkbook
        val createHelper = workbook.getCreationHelper

        val sheet = workbook.createSheet("slothbike_payments")

        val headerFont = workbook.createFont
        headerFont.setBold(true)

        val headerCellStyle = workbook.createCellStyle
        headerCellStyle.setFont(headerFont)

        val headerRow = sheet.createRow(0)

        columns.zipWithIndex.map { c =>
          var cell = headerRow.createCell(c._2)
          cell.setCellValue(c._1)
          cell.setCellStyle(headerCellStyle)
        }

        searchResult.zipWithIndex.map { sr =>
          var row = sheet.createRow(sr._2 + 1)

          row.createCell(0).setCellValue(sr._1.studentId)
          row.createCell(1).setCellValue(sr._1.firstName + " " + sr._1.lastName)
          row.createCell(2).setCellValue(sr._1.major)
          row.createCell(3).setCellValue(sr._1.overtimeFine)
          row.createCell(4).setCellValue(sr._1.defectFine)
          row.createCell(5).setCellValue(sr._1.defectFine + sr._1.overtimeFine)
        }

        columns.zipWithIndex.map { c =>
          sheet.autoSizeColumn(c._2);
        }

        val tempFile = File.createTempFile(s"slothbike_payments-${new java.util.Date().toString}", ".xlsx", null)
        val fileOut = new FileOutputStream(tempFile)
        workbook.write(fileOut)
        fileOut.close()

        workbook.close()

        Ok.sendFile(tempFile)
      }).extract

  }
}