package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import repositories._

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class ReportController @Inject()(cc: ControllerComponents, bikeRepo: BikeRepository,
                                 bikeStatusRepository: BikeStatusRepository, stationRepository: StationRepository,
                                 historyRepository: HistoryRepository, studentRepository: StudentRepository)
                                (implicit assetsFinder: AssetsFinder, ec: ExecutionContext)
  extends AbstractController(cc) {

  def viewReport = Action.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.reports()))
  }

}
