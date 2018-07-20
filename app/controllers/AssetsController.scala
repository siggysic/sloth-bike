package controllers

import java.util.UUID

import javax.inject.{Inject, _}
import models._
import org.apache.poi.ss.usermodel.{DataFormatter, WorkbookFactory}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import repositories.{BikeRepository, BikeStatusRepository, HistoryRepository, StationRepository}

import scala.collection.JavaConversions.`deprecated iterableAsScalaIterable`
import scala.collection.convert.ImplicitConversions.`iterator asScala`
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AssetsController @Inject()(cc: ControllerComponents, bikeRepo: BikeRepository,
                                 bikeStatusRepository: BikeStatusRepository, stationRepository: StationRepository,
                                 historyRepository: HistoryRepository)
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

  def viewInsertAssets = Action.async {
    for {
      bStatus <- bikeStatusRepository.getStatus
      st <- stationRepository.getStations
    } yield Ok(views.html.assetsInsert(bikeForm, fields, bStatus, st))
  }

  def viewAssets(page: Int = pageSize.page) = Action.async {
    for {
      bikes <- bikeRepo.getBikesRelational(BikeQuery(None, page, pageSize.size))
      status <- bikeStatusRepository.getStatus
    } yield Ok(views.html.assets(bikeSearchForm, bikes, fields, pageSize.copy(page = page), status))
  }

  def viewDetailAssets(id: String, page: Int = pageSize.page) = Action.async { implicit request: Request[AnyContent] =>
    for {
      bike <- bikeRepo.getBike(id)
      history <- historyRepository.getHistories(HistoryQuery(Some(id), None, None, None, None, page, pageSize.size))
    } yield bike match {
      case Some(b) =>
        val transformHistory: Seq[(Int, History, Option[BikeStatus], Option[(UUID, Option[Int])])] =
          history.map(h => (h._1, h._2._1._1._1._1, h._2._1._1._2, h._2._1._2))
        Ok(views.html.assetsDetail(b, transformHistory, pageSize.copy(page = page)))
      case None => BadRequest(views.html.exception("Database exception."))
    }
  }

  def viewEditAssets(id: String, page: Int = pageSize.page) = Action.async { implicit request: Request[AnyContent] =>
    for {
      bike <- bikeRepo.getBike(id)
      status <- bikeStatusRepository.getStatus
      st <- stationRepository.getStations
    } yield bike match {
      case Some(b) => Ok(views.html.assetsEdit(bikeForm.fillAndValidate(b._1.toBikeRequest), fields, status, st))
      case None => BadRequest(views.html.exception("Database exception."))
    }
  }

  def viewImportAssets = Action.async { implicit request: Request[AnyContent] =>
    for {
      status <- bikeStatusRepository.getStatus
      st <- stationRepository.getStations
    } yield Ok(views.html.assetsImport(bikeImportForm, fields, status, st))
  }

  def insertAssets = Action.async { implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[BikeRequest] =>
      for {
        bStatus <- bikeStatusRepository.getStatus
        st <- stationRepository.getStations
      } yield BadRequest(views.html.assetsInsert(formWithErrors, fields, bStatus, st))
    }

    val successFunction = { data: BikeRequest =>
      bikeRepo.create(data.toBike).flatMap {
        case 1 =>  for {
          bikes <- bikeRepo.getBikesRelational(BikeQuery(None, pageSize.page, pageSize.size))
          status <- bikeStatusRepository.getStatus
        } yield Ok(views.html.assets(bikeSearchForm, bikes, fields, pageSize, status))
        case _ => Future.successful(BadRequest(views.html.exception("Database exception.")))
      }
    }

    val formValidationResult: Form[BikeRequest] = bikeForm.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }

  def editAssets = Action.async { implicit request: Request[AnyContent] =>
    val errorFunction = { formWithErrors: Form[BikeRequest] =>
      for {
        bStatus <- bikeStatusRepository.getStatus
        st <- stationRepository.getStations
      } yield BadRequest(views.html.assetsEdit(formWithErrors, fields, bStatus, st))
    }

    val successFunction = { data: BikeRequest =>
      bikeRepo.update(data.toBike).flatMap {
        case 1 =>  for {
          bikes <- bikeRepo.getBikesRelational(BikeQuery(None, pageSize.page, pageSize.size))
          status <- bikeStatusRepository.getStatus
        } yield Ok(views.html.assets(bikeSearchForm, bikes, fields, pageSize, status))
        case _ => Future.successful(BadRequest(views.html.exception("Database exception.")))
      }
    }

    val formValidationResult: Form[BikeRequest] = bikeWithNoValidateDateForm.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }

  def uploadAssets = Action.async(parse.multipartFormData) { implicit request =>
    val errorFunction = { formWithErrors: Form[BikeImport] =>
      for {
        status <- bikeStatusRepository.getStatus
        st <- stationRepository.getStations
      } yield BadRequest(views.html.assetsImport(bikeImportForm, fields, status, st))
    }

    val successFunction = { data: BikeImport =>
      request.body.file("fileImport").map {
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
              if(row._2 == 0 && !row._1.equals(headers)) None
              else Some(BikeRequest(None, row._1(1), row._1(2), data.lotNo, row._1(3), data.detail,
                new java.util.Date, row._1(0), data.statusId, data.stationId).toBike)
            }
            bikes.find(_ == None) match {
              case None => Future.successful(Ok(""))
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
      case _ => searchAssets(formSearch, bikeSearch, page)
    }
  }

  private def searchAssets(formSearch: Form[BikeSearch], bikeSearch: BikeSearch, page: Int = pageSize.page) = {
    val validateStatusId: BikeSearch = bikeSearch.statusId match {
      case Some(v) if v < 0 => bikeSearch.copy(statusId = None)
      case _ => bikeSearch
    }
    for {
      searchResult <- bikeRepo.searchBikes(validateStatusId, BikeQuery(None, page, pageSize.size))
      status <- bikeStatusRepository.getStatus
    } yield Ok(views.html.assets(formSearch, searchResult, fields, pageSize.copy(page = page), status))
  }

  private def printBarCode(formSearch: Form[BikeSearch], bikeSearch: BikeSearch) = {
    val validateStatusId: BikeSearch = bikeSearch.statusId match {
      case Some(v) if v < 0 => bikeSearch.copy(statusId = None)
      case _ => bikeSearch
    }
    for {
      searchResult <- bikeRepo.searchBikes(validateStatusId, BikeQuery(None, pageSize.page, pageSize.size * 100))
      status <- bikeStatusRepository.getStatus
    } yield Ok(views.html.assetsBarcode(formSearch, searchResult, fields))
  }

}