package controllers.api

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, Controller, Request}
import repositories.{PaymentRepository, StationRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class StationController @Inject()(stationRepository: StationRepository) extends Controller {
  def getStations = Action.async { implicit request: Request[AnyContent] =>
    for {
      stations <- stationRepository.getStations
    } yield Ok()
  }
}
