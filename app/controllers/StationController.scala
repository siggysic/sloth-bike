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

  val queryForm: Form[StationQuery] = Form(
    mapping(
      "name" -> optional(text),
      "available" -> optional(number),
      "page" -> default(number(min = 1), 1),
      "pageSize" -> default(number, 10)
    )(StationQuery.apply)(StationQuery.unapply)
  )

  val insertForm: Form[Station] = Form(
    mapping(
      "id" -> optional(number),
      "stationName" -> text.verifying(Contraints.validateText),
      "location" -> text.verifying(Contraints.validateText)
    )(Station.apply)(Station.unapply)
  )

  def viewStations
  (name: Option[String] = None, available: Option[Int] = None, page: Int = 1) = Action.async {

    val actualPage = page match {
      case p if p >= 1 => p
      case _ => 1
    }

    val currentForm = queryForm.fill(StationQuery(name, available, actualPage))

    stationRepository.getStations(StationQuery(name, available, actualPage)).map { r =>
      val (stations, totalStation) = r
      Ok(views.html.stations(stations.map(s => StationResult(s._1, s._2, s._3, s._4)), totalStation, currentForm))
    }
  }

  def filterStationAction = Action { implicit request: Request[AnyContent] =>

    val failureFn = { formWithError: Form[StationQuery] =>
      val name = formWithError.data.get("name")
      val available = formWithError.data.get("available").map(_.toInt)
      val page = formWithError.data.get("page").map(_.toInt).getOrElse(1)
      Redirect(routes.StationController.viewStations(name, available, page))
    }

    val successFn = { query: StationQuery =>
      Redirect(routes.StationController.viewStations(query.name, query.available))
    }

    queryForm.bindFromRequest().fold(failureFn, successFn)

  }

  def viewAddStation = Action {
    Ok(views.html.addStation(insertForm))
  }

  def addStation = Action.async { implicit request: Request[AnyContent] =>

    val failure = { formWithFailure: Form[Station] =>
      Future.successful(BadRequest(views.html.addStation(formWithFailure)))
    }

    val success = { station: Station =>
      stationRepository.create(station).flatMap { _ =>
        stationRepository.getStations(StationQuery(None, None, 1)).map( s =>
          Redirect(routes.StationController.viewStations())
        )
      }
    }

    insertForm.bindFromRequest().fold(
      failure,
      success
    )
  }
}
