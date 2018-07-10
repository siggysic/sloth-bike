package controllers

import javax.inject.{Inject, Singleton}
import models.{Contraints, Station, StationQuery, StationResult}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import repositories.StationRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class StationController @Inject()(stationRepository: StationRepository, cc: ControllerComponents)(implicit assets: AssetsFinder, ec: ExecutionContext)
  extends AbstractController(cc) {

  val form: Form[Station] = Form(
    mapping(
      "id" -> optional(number),
      "stationName" -> text.verifying(Contraints.validateText),
      "location" -> text.verifying(Contraints.validateText)
    )(Station.apply)(Station.unapply)
  )

  def viewStations(page: Int = 1) = Action.async {
      stationRepository.getStations(StationQuery(None, None, page,1)).map { r =>
        val (stations, totalStation) = r
        Ok(views.html.stations(stations.map(s => StationResult(s._1, s._2, s._3, s._4)), totalStation))
      }
  }

  def viewAddStation = Action {
    Ok(views.html.addStation(form))
  }

  def addStation = Action.async { implicit request: Request[AnyContent] =>

    val failure = { formWithFailure: Form[Station] =>
      Future.successful(BadRequest(views.html.addStation(formWithFailure)))
    }

    val success = { station: Station =>
      stationRepository.create(station).flatMap { _ =>
        stationRepository.getStations(StationQuery(None, None, 1, 1)).map( s =>
          Redirect(routes.StationController.viewStations())
        )
      }
    }

    form.bindFromRequest().fold(
      failure,
      success
    )
  }
}
