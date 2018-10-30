package controllers

import java.io.{File, FileOutputStream}
import java.sql.Timestamp
import java.util.UUID

import akka.stream.scaladsl.StreamConverters
import cats.data.EitherT
import javax.inject.{Inject, _}
import models._
import org.apache.poi.ss.usermodel.{DataFormatter, WorkbookFactory}
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import repositories._

import scala.collection.JavaConversions.`deprecated iterableAsScalaIterable`
import scala.collection.convert.ImplicitConversions.`iterator asScala`
import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import utils.Helpers.EitherHelper.{CatchDatabaseExp, ExtractEitherT}


@Singleton
class AssetsController @Inject()(cc: ControllerComponents, bikeRepo: BikeRepository,
                                 bikeStatusRepository: BikeStatusRepository, stationRepository: StationRepository,
                                 historyRepository: HistoryRepository, studentRepository: StudentRepository)
                                (implicit assetsFinder: AssetsFinder, ec: ExecutionContext)
  extends AbstractController(cc) {

  val bikeForm: Form[BikeRequest] = Form (
    mapping(
      "id" -> optional(text),
      "keyBarcode" -> text.verifying(Contraints.validateText),
      "referenceId" -> text.verifying(Contraints.validateText),
      "lotNo" -> text.verifying(Contraints.validateText),
      "licensePlate" -> text.verifying(Contraints.validateText),
      "detail" -> optional(text),
      "arrivalDate" -> date.verifying(Contraints.validateDate),
      "pieceNo" -> text.verifying(Contraints.validateText),
      "statusId" -> number,
      "stationId" -> number
    )(BikeRequest.apply)(BikeRequest.unapply)
  )

  val bikeWithFieldForm: Form[BikeRequestWithField] = Form (
    mapping(
      "id" -> optional(text),
      "keyBarcode" -> text.verifying(Contraints.validateText),
      "referenceId" -> text.verifying(Contraints.validateText),
      "lotNo" -> text.verifying(Contraints.validateText),
      "licensePlate" -> text.verifying(Contraints.validateText),
      "detail" -> optional(text),
      "arrivalDate" -> date.verifying(Contraints.validateDate),
      "pieceNo" -> text.verifying(Contraints.validateText),
      "statusId" -> number,
      "stationId" -> number,
      "field" -> optional(text)
    )(BikeRequestWithField.apply)(BikeRequestWithField.unapply)
  )

  val bikeWithNoValidateDateForm: Form[BikeRequest] = Form (
    mapping(
      "id" -> optional(text),
      "keyBarcode" -> text.verifying(Contraints.validateText),
      "referenceId" -> text.verifying(Contraints.validateText),
      "lotNo" -> text.verifying(Contraints.validateText),
      "licensePlate" -> text.verifying(Contraints.validateText),
      "detail" -> optional(text),
      "arrivalDate" -> date,
      "pieceNo" -> text.verifying(Contraints.validateText),
      "statusId" -> number,
      "stationId" -> number
    )(BikeRequest.apply)(BikeRequest.unapply)
  )

  val bikeSearchForm: Form[BikeSearch] = Form (
    mapping(
      "keyBarcode" -> optional(text),
      "referenceId" -> optional(text),
      "lotNo" -> optional(text),
      "licensePlate" -> optional(text),
      "pieceNo" -> optional(text),
      "statusId" -> optional(number)
    )(BikeSearch.apply)(BikeSearch.unapply)
  )

  var bikeImportForm: Form[BikeImport] = Form (
    mapping(
      "lotNo" -> text.verifying(Contraints.validateText),
      "detail" -> optional(text),
      "statusId" -> number,
      "stationId" -> number
    )(BikeImport.apply)(BikeImport.unapply)
  )

  val fields = BikeFields()
  val pageSize = PageSize()

  def viewInsertAssets = Action.async { implicit request =>
    (
      for {
        bStatus <- bikeStatusRepository.getStatus.dbExpToEitherT
        st <- stationRepository.getStations.dbExpToEitherT
      } yield Ok(views.html.assetsInsert(bikeWithFieldForm, fields, bStatus, st))
    ).extract
  }

  def viewAssets(page: Int = pageSize.page) = Action.async { implicit request =>
    (
      for {
        bikes <- bikeRepo.getBikesRelational(BikeQuery(None, page, pageSize.size)).dbExpToEitherT
        status <- bikeStatusRepository.getStatus.dbExpToEitherT
      } yield Ok(views.html.assets(bikeSearchForm, bikes, fields, pageSize.copy(page = page), status))
    ).extract
  }

  def viewDetailAssets(id: String, page: Int = pageSize.page) = Action.async { implicit request: Request[AnyContent] =>
    (
      for {
        bike <- bikeRepo.getBike(id).dbExpToEitherT
        history <- historyRepository.getHistories(HistoryQuery(Some(id), None, None, None, None, page, pageSize.size)).dbExpToEitherT
      } yield bike match {
        case Some(b) =>
          val transformHistory: Seq[(Int, History, Option[BikeStatus], Option[(String, Option[Int])])] =
            history.map(h => (h._1, h._2._1._1._1._1, h._2._1._1._2, h._2._1._2))
          Ok(views.html.assetsDetail(b, transformHistory, pageSize.copy(page = page)))
        case None => BadRequest(views.html.exception("Database exception."))
      }
    ).extract
  }

  def viewEditAssets(id: String, page: Int = pageSize.page) = Action.async { implicit request: Request[AnyContent] =>
    (
      for {
        bike <- bikeRepo.getBike(id).dbExpToEitherT
        status <- bikeStatusRepository.getStatus.dbExpToEitherT
        st <- stationRepository.getStations.dbExpToEitherT
        history <- historyRepository.getLastActionOfBike(id, bike.map(_._1.statusId).getOrElse(0)).dbExpToEitherT
      } yield (bike, history) match {
        case (Some(b), Some((h, bs))) =>
          if(b._1.statusId == 2)
            Ok(views.html.assetsEdit(bikeWithFieldForm.fillAndValidate(b._1.toBikeRequest.copy(field = h.studentId)), fields, status, st))
          else if(b._1.statusId == 3)
            Ok(views.html.assetsEdit(bikeWithFieldForm.fillAndValidate(b._1.toBikeRequest.copy(field = h.remark)), fields, status, st))
          else Ok(views.html.assetsEdit(bikeWithFieldForm.fillAndValidate(b._1.toBikeRequest), fields, status, st))
        case (Some(b), None) => Ok(views.html.assetsEdit(bikeWithFieldForm.fillAndValidate(b._1.toBikeRequest), fields, status, st))
        case (None, _) => BadRequest(views.html.exception("Database exception."))
      }
    ).extract
  }

  def viewImportAssets = Action.async { implicit request: Request[AnyContent] =>
    (
      for {
        status <- bikeStatusRepository.getStatus.dbExpToEitherT
        st <- stationRepository.getStations.dbExpToEitherT
      } yield Ok(views.html.assetsImport(bikeImportForm, fields, status, st))
    ).extract
  }

  def insertAssets = Action.async { implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[BikeRequestWithField] =>
      (
        for {
          bStatus <- bikeStatusRepository.getStatus.dbExpToEitherT
          st <- stationRepository.getStations.dbExpToEitherT
        } yield {
          formWithErrors("statusId").value match {
            case Some("2") => BadRequest(views.html.assetsInsert(
              formWithErrors.withError(FormError("field", "Student id is required" :: Nil)), fields, bStatus, st))
            case Some("3") => BadRequest(views.html.assetsInsert(
              formWithErrors.withError(FormError("field", "Remark is required" :: Nil)), fields, bStatus, st))
            case _ => BadRequest(views.html.assetsInsert(formWithErrors, fields, bStatus, st))
          }
        }
      ).extract
    }

    val successFunction = { data: BikeRequestWithField =>
      (
        for {
          status <- bikeStatusRepository.getStatus.dbExpToEitherT
          st <- stationRepository.getStations.dbExpToEitherT
          validate <- {
            val preBike = data.toBike
            val result = data.statusId match {
              case 1 => bikeRepo.create(preBike).map {
                case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                case _ => Left(BadRequest(views.html.exception("Database exception.")))
              }
              case 2 => data.field match {
                case Some(value) => studentRepository.getStudentById(value).flatMap {
                  case Right(Some(student)) if student.status.toLowerCase == "active" => bikeRepo.create(preBike).flatMap {
                    case 1 =>
                      val history = History(
                        id = UUID.randomUUID().toString,
                        studentId = Some(student.id),
                        remark = None,
                        borrowDate = Some(new Timestamp(System.currentTimeMillis())),
                        returnDate = None,
                        station = Some(data.stationId),
                        bikeId = preBike.id,
                        paymentId = None,
                        statusId = data.statusId
                      )
                      historyRepository.create(history).map {
                        case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                        case _ => Left(BadRequest(views.html.exception("Database exception.")))
                      }
                    case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
                  }
                  case Right(Some(_)) =>
                    Future.successful(Left(BadRequest(views.html.assetsInsert(bikeWithFieldForm.fillAndValidate(data).withError(FormError("field", "Student status is not ready to borrow" :: Nil)), fields, status, st))))
                  case Right(None) =>
                    Future.successful(Left(BadRequest(views.html.assetsInsert(bikeWithFieldForm.fillAndValidate(data).withError(FormError("field", "Student not found" :: Nil)), fields, status, st))))
                  case Left(_) => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
                }
                case None => Future.successful(Left(BadRequest(views.html.assetsInsert(bikeWithFieldForm.fillAndValidate(data).withError(FormError("field", "Student id is required" :: Nil)), fields, status, st))))
              }
              case 3 => data.field match {
                case Some(value) => bikeRepo.create(preBike).flatMap {
                  case 1 =>
                    val history = History(
                      id = UUID.randomUUID().toString,
                      studentId = None,
                      remark = Some(value),
                      borrowDate = Some(new Timestamp(System.currentTimeMillis())),
                      returnDate = None,
                      station = Some(data.stationId),
                      bikeId = preBike.id,
                      paymentId = None,
                      statusId = data.statusId
                    )
                    historyRepository.create(history).map {
                      case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                      case _ => Left(BadRequest(views.html.exception("Database exception.")))
                    }
                  case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
                }
                case None => Future.successful(Left(BadRequest(views.html.assetsInsert(bikeWithFieldForm.fillAndValidate(data).withError(FormError("field", "Remark is required" :: Nil)), fields, status, st))))
              }
              case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
            }
            EitherT(result)
          }
        } yield validate
      ).extract
    }

    val formValidationResult: Form[BikeRequestWithField] = bikeWithFieldForm.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }

  def editAssets = Action.async { implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[BikeRequestWithField] =>
      (
        for {
          bStatus <- bikeStatusRepository.getStatus.dbExpToEitherT
          st <- stationRepository.getStations.dbExpToEitherT
        } yield {
          formWithErrors("statusId").value match {
            case Some("2") => BadRequest(views.html.assetsEdit(
              formWithErrors.withError(FormError("field", "Student id is required" :: Nil)), fields, bStatus, st))
            case Some("3") => BadRequest(views.html.assetsEdit(
              formWithErrors.withError(FormError("field", "Remark is required" :: Nil)), fields, bStatus, st))
            case _ => BadRequest(views.html.assetsEdit(formWithErrors, fields, bStatus, st))
          }
        }
      ).extract
    }

    val successFunction = { data: BikeRequestWithField =>
      (
        for {
          oldBike <- bikeRepo.getBike(data.id.getOrElse("")).dbExpToEitherT
          status <- bikeStatusRepository.getStatus.dbExpToEitherT
          st <- stationRepository.getStations.dbExpToEitherT
          validate <- {
            val preBike = data.toBike
            val result = data.statusId match {
              case 1 => bikeRepo.update(preBike).map {
                case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                case _ => Left(BadRequest(views.html.exception("Database exception.")))
              }
              case 2 => data.field match {
                case Some(value) => studentRepository.getStudentById(value).flatMap {
                  case Right(Some(student)) => bikeRepo.update(preBike).flatMap {
                    case 1 =>
                      if(oldBike.map(_._1.statusId != 2).forall(identity)) {
                        val history = History(
                          id = UUID.randomUUID().toString,
                          studentId = Some(student.id),
                          remark = None,
                          borrowDate = Some(new Timestamp(System.currentTimeMillis())),
                          returnDate = None,
                          station = Some(data.stationId),
                          bikeId = preBike.id,
                          paymentId = None,
                          statusId = data.statusId
                        )
                        historyRepository.create(history).map {
                          case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                          case _ => Left(BadRequest(views.html.exception("Database exception.")))
                        }
                      } else Future.successful(Right(Redirect(routes.AssetsController.viewAssets())))

                    case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
                  }
                  case Right(None) =>
                    Future.successful(Left(BadRequest(views.html.assetsEdit(bikeWithFieldForm.fillAndValidate(data).withError(FormError("field", "Student not found" :: Nil)), fields, status, st))))
                  case Left(_) => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
                }
                case None => Future.successful(Left(BadRequest(views.html.assetsEdit(bikeWithFieldForm.fillAndValidate(data).withError(FormError("field", "Student id is required" :: Nil)), fields, status, st))))
              }
              case 3 => data.field match {
                case Some(value) => bikeRepo.update(preBike).flatMap {
                  case 1 =>
                    if(oldBike.map(_._1.statusId != 3).forall(identity)) {
                      val history = History(
                        id = UUID.randomUUID().toString,
                        studentId = None,
                        remark = Some(value),
                        borrowDate = Some(new Timestamp(System.currentTimeMillis())),
                        returnDate = None,
                        station = Some(data.stationId),
                        bikeId = preBike.id,
                        paymentId = None,
                        statusId = data.statusId
                      )
                      historyRepository.create(history).map {
                        case 1 => Right(Redirect(routes.AssetsController.viewAssets()))
                        case _ => Left(BadRequest(views.html.exception("Database exception.")))
                      }
                    } else Future.successful(Right(Redirect(routes.AssetsController.viewAssets())))
                  case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
                }
                case None => Future.successful(Left(BadRequest(views.html.assetsEdit(bikeWithFieldForm.fillAndValidate(data).withError(FormError("field", "Remark is required" :: Nil)), fields, status, st))))
              }
              case _ => Future.successful(Left(BadRequest(views.html.exception("Database exception."))))
            }
            EitherT(result)
          }
        } yield validate
      ).extract
    }

    val formValidationResult: Form[BikeRequestWithField] = bikeWithFieldForm.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }

  def uploadAssets = Action.async { implicit request: Request[AnyContent] =>
    val reqMul = request.map(s => s.asMultipartFormData.get)
    val errorFunction = { formWithErrors: Form[BikeImport] =>
      (
        for {
          status <- bikeStatusRepository.getStatus.dbExpToEitherT
          st <- stationRepository.getStations.dbExpToEitherT
        } yield BadRequest(views.html.assetsImport(formWithErrors, fields, status, st))
      ).extract
    }

    val successFunction = { data: BikeImport =>
      reqMul.body.file("fileImport").map {
        case FilePart(key, filename, contentType, file) =>
          try {
            val headers = List("เลขครุภัณฑ์", "Barcode", "Reference ID", "ป้ายทะเบียน")

            val workbook = WorkbookFactory.create(file.toFile)
            val formatter = new DataFormatter()
            val excels: Iterable[List[String]] = for {
              (sheet, i) <- workbook.zipWithIndex
              row <- sheet
            } yield row.iterator().toList.map(formatter.formatCellValue)
            val bikes = excels.zipWithIndex.map{ row =>
              if(row._2 == 0 && row._1.equals(headers)) None
              else Some(BikeRequest(None, row._1(1), row._1(2), data.lotNo, row._1(3), data.detail,
                new java.util.Date, row._1(0), data.statusId, data.stationId).toBike)
            }
            bikes.filter(_ == None).size match {
              case 1 => bikeRepo.createBulk(bikes.flatten.toList).map { _ =>
                Redirect(routes.AssetsController.viewAssets())
              }
              case _ => Future.successful(BadRequest(views.html.exception("File upload exception.")))
            }
          } catch {
            case _: Exception => Future.successful(BadRequest(views.html.exception("File upload exception.")))
          }
        case _ => Future.successful(BadRequest(views.html.exception("File upload exception.")))
      }.getOrElse(
        Future.successful(BadRequest(views.html.exception("File upload exception.")))
      )
    }

    val formImport: Form[BikeImport] = bikeImportForm.bindFromRequest

    formImport.fold(
      errorFunction,
      successFunction
    )
  }

  def searchOrPrintBarcode(bikeSearch: BikeSearch, page: Int = pageSize.page) = Action.async { implicit request: Request[AnyContent] =>
    val formSearch: Form[BikeSearch] = bikeSearchForm.bindFromRequest
    val action = formSearch("submit-action")
    action.value match {
      case Some("print-barcode") => printBarCode(formSearch, bikeSearch)
      case Some("export") => export(formSearch, bikeSearch)
      case _ => searchAssets(formSearch, bikeSearch, page)
    }
  }

  def viewTemplateAssetFile = Action.async{
    Future.successful(Ok.sendFile(new java.io.File("public/template_asset_import.xlsx")))
  }

  private def searchAssets(formSearch: Form[BikeSearch], bikeSearch: BikeSearch, page: Int = pageSize.page)(implicit request: Request[AnyContent]) = {
    val validateStatusId: BikeSearch = bikeSearch.statusId match {
      case Some(v) if v < 0 => bikeSearch.copy(statusId = None)
      case _ => bikeSearch
    }
    (
      for {
        searchResult <- bikeRepo.searchBikes(validateStatusId, BikeQuery(None, page, pageSize.size)).dbExpToEitherT
        status <- bikeStatusRepository.getStatus.dbExpToEitherT
      } yield Ok(views.html.assets(formSearch, searchResult, fields, pageSize.copy(page = page), status))
    ).extract
  }

  private def printBarCode(formSearch: Form[BikeSearch], bikeSearch: BikeSearch)(implicit request: Request[AnyContent]) = {
    val validateStatusId: BikeSearch = bikeSearch.statusId match {
      case Some(v) if v < 0 => bikeSearch.copy(statusId = None)
      case _ => bikeSearch
    }
    (
      for {
        searchResult <- bikeRepo.searchBikes(validateStatusId, BikeQuery(None, pageSize.page, pageSize.size * 100)).dbExpToEitherT
      } yield Ok(views.html.assetsBarcode(formSearch, searchResult, fields))
    ).extract
  }

  private def export(formSearch: Form[BikeSearch], bikeSearch: BikeSearch)(implicit request: Request[AnyContent]) = {
    val columns = List("เลขครุภัณฑ์", "Barcode", "Reference ID", "ป้ายทะเบียน", "Lot number", "Remark", "Detail", "Arrival date", "Status", "Station")

    val validateStatusId: BikeSearch = bikeSearch.statusId match {
      case Some(v) if v < 0 => bikeSearch.copy(statusId = None)
      case _ => bikeSearch
    }

    (
      for {
        searchResult <- bikeRepo.searchBikes(validateStatusId, BikeQuery(None, pageSize.page, pageSize.size * 100)).dbExpToEitherT
      } yield {

        val workbook = new XSSFWorkbook
        val createHelper = workbook.getCreationHelper

        val sheet = workbook.createSheet("slothbike")

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

          row.createCell(0).setCellValue(sr._1._2.pieceNo)
          row.createCell(1).setCellValue(sr._1._2.keyBarcode.getOrElse(""))
          row.createCell(2).setCellValue(sr._1._2.referenceId)
          row.createCell(3).setCellValue(sr._1._2.licensePlate)
          row.createCell(4).setCellValue(sr._1._2.lotNo)
          row.createCell(5).setCellValue(sr._1._2.remark.getOrElse(""))
          row.createCell(6).setCellValue(sr._1._2.detail.getOrElse(""))
          row.createCell(7).setCellValue(sr._1._2.arrivalDate.toString)
          row.createCell(8).setCellValue(sr._1._3.status)
          row.createCell(9).setCellValue(s"${sr._1._4.name}, ${sr._1._4.location}")
        }

        columns.zipWithIndex.map { c =>
          sheet.autoSizeColumn(c._2);
        }

        val tempFile = File.createTempFile(s"slothbike-${new java.util.Date().toString}", ".xlsx", null)
        val fileOut = new FileOutputStream(tempFile)
        workbook.write(fileOut)
        fileOut.close()

        workbook.close()

        Ok.sendFile(tempFile)
      }
    ).extract
  }

}