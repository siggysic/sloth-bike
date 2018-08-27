package controllers.api

import java.util.concurrent.TimeUnit

import javax.inject.Inject
import models.{FineResult, Response}
import play.api.mvc.{Action, ControllerComponents}
import repositories.HistoryRepository

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.TimeUnit

class HistoryController @Inject()(cc: ControllerComponents, historyRepository: HistoryRepository)
                                 (implicit ec: ExecutionContext) extends Response {

  def getStudentFine(studentId: String) = Action.async {
    import models.mappers.HistoryMapper._
    val fine = historyRepository.getLastActionOfStudent(studentId).map {
      case Right(data) =>
        val result = data.map(_.toQueryClass)
        val historyId = result.map(_.id).getOrElse("")
        val borrowedDate = result.flatMap(_.borrowDate)
        val fine = borrowedDate.map(b => getDateDiff(b.getTime, System.currentTimeMillis(), TimeUnit.HOURS).toInt).getOrElse(0)
        Right(FineResult(historyId, fine))
      case Left(ex) =>
        Left(ex)
    }

    response(fine)
  }

  private def getDateDiff(diff: Long, base: Long, timeUnit: TimeUnit): Long = {
    val result = Math.abs(base - diff)
    timeUnit.convert(result, TimeUnit.MILLISECONDS)
  }
}
