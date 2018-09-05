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

  import utils.Helpers._

  def getStudentFine(studentId: String) = Action.async {
    import models.mappers.HistoryMapper._
    val fine = historyRepository.getLastActionOfStudent(studentId).map {
      case Right(data) =>
        data match {
          case Some(value) =>
            val result = value.toQueryClass
            val historyId = result.id
            val borrowedDate = result.borrowDate
            val arrivalDate = borrowedDate.map(_.toLocalDateTime.toLocalDate)
            val now = java.time.LocalDate.now
            val diff = java.time.Period.between(arrivalDate.getOrElse(now), now)
            val fine = Calculate.payment(diff.getDays)
            Right(FineResult(historyId, fine, true))
          case None => Right(FineResult("", 0, false))
        }
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
