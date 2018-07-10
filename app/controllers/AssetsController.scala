package controllers

import javax.inject.{Inject, _}
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import repositories.{BikeRepository, BikeStatusRepository, StationRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AssetsController @Inject()(cc: ControllerComponents, bikeRepo: BikeRepository,
                                 bikeStatusRepository: BikeStatusRepository, stationRepository: StationRepository)
                                (implicit assetsFinder: AssetsFinder, ec: ExecutionContext)
  extends AbstractController(cc) {

  val bikeForm: Form[BikeRequest] = Form (
    mapping(
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

  val fields = BikeFields()
  val bikesStatus = bikeStatusRepository.getStatus
  val stations = stationRepository.getStations
  val pageSize = PageSize()

  def viewInsertAssets = Action.async {
    for {
      bStatus <- bikesStatus
      st <- stations
    } yield Ok(views.html.assetsInsert(bikeForm, fields, bStatus, st))
  }

  def viewAssets(page: Int = pageSize.page) = Action.async {
    for {
      bikes <- bikeRepo.getBikesRelational(BikeQuery(None, page, pageSize.size))
      status <- bikeStatusRepository.getStatus
    } yield Ok(views.html.assets(bikes, fields, pageSize.copy(page = page), status))
  }

  def insertAssets = Action.async { implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[BikeRequest] =>
      for {
        bStatus <- bikesStatus
        st <- stations
      } yield BadRequest(views.html.assetsInsert(formWithErrors, fields, bStatus, st))
    }

    val successFunction = { data: BikeRequest =>
      bikeRepo.create(data.toBike).flatMap {
        case 1 =>  for {
          bikes <- bikeRepo.getBikesRelational(BikeQuery(None, pageSize.page, pageSize.size))
          status <- bikeStatusRepository.getStatus
        } yield Ok(views.html.assets(bikes, fields, pageSize, status))
        case _ => Future.successful(BadRequest(views.html.exception("Database exception.")))
      }
    }

    val formValidationResult: Form[BikeRequest] = bikeForm.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }

  def searchAssets = Action.async { implicit request: Request[AnyContent] =>
    Future(Ok(""))
  }

}