package controllers.api

import javax.inject.Inject
import models._
import net.liftweb.json.Extraction
import net.liftweb.json.JsonAST.{JField, JObject, JString}
import net.liftweb.json.JsonDSL._
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.mvc._
import repositories.StationRepository
import utils.ClaimSet

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StationController @Inject()(stationRepository: StationRepository) extends Response {

  import utils.Helpers.Authentication._

  val loginStationForm: Form[LoginStation] = Form (
    mapping(
      "station_id" -> number.verifying(Contraints.validateNumberWithField("station_id"))
    )(LoginStation.apply)(LoginStation.unapply)
  )

  def getStations = Action.async {
    val stations: Future[Either[CustomException, Seq[Station]]] = for {
      st <- stationRepository.getStations
    } yield Right(st)

    response(stations)
  }

  def loginStations = Action.async { implicit request: Request[AnyContent] =>
    val errorFunction = { formWithErrors: Form[LoginStation] =>
      response(Future.successful(Left(CustomException(formWithErrors.errors.map(_.message), 400))))
    }

    val successFunction = { data: LoginStation =>
      val stations: Future[Either[CustomException, ClaimSet]] = for {
        st <- stationRepository.getStation(data.station_id)
      } yield st match {
        case Some(value) => Right(ClaimSet(value.id.get, value.name, value.location))
        case None => Left(CustomException("Station not found" :: Nil, 404))
      }
      response(stations.map(_.right.map(s => ("token" -> Extraction.decompose(encode(s))) ~ JObject(Nil))))
    }

    val formValidationResult: Form[LoginStation] = loginStationForm.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }

}
