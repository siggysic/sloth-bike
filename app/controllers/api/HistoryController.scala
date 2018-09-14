package controllers.api

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

import javax.inject.Inject
import models.{FineResult, Response}
import org.joda.time.DateTime
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

  def getPopularityChart(startDate: Option[Long], endDate: Option[Long]) = Action.async {
    case class DateChartQuery(id: String, hour: Int)
    case class DateChartResponse(count: Double, range: String)
    val resp = historyRepository.getAll(startDate.map(new Timestamp(_)), endDate.map(new Timestamp(_))) map {
      case Right(histories) =>
        val bor = histories.filter(_.borrowDate.isDefined).map(h => DateChartQuery(h.id, new DateTime(h.borrowDate.get).getHourOfDay))
        val ret = histories.filter(_.returnDate.isDefined).map(h => DateChartQuery(h.id, new DateTime(h.returnDate.get).getHourOfDay))
        val sum = bor.size + ret.size
        val r = (8 until 21).map { i =>
          if (sum == 0) DateChartResponse(0, s"$i-${i+1}")
          else {
            val borCount = bor.count(_.hour == i)
            val retCount = ret.count(_.hour == i)
            val percentage = (borCount + retCount) * 100.0 / (8 until 21).sum
            DateChartResponse(percentage.round, s"$i-${i+1}")
          }
        }
        Right(r)
      case Left(ex) =>
        Left(ex)
    }
    response(resp)
  }

  def convertDateToLong(str: String) = {
    val dateformat = new SimpleDateFormat("yyyy-mm-dd")
    if (str.isEmpty) None
    else {
      val d = dateformat.parse(str)
      Some(d.getTime)
    }
  }

  def convertLongtoDateString(long: Long) = {
    val dateformat = new SimpleDateFormat("yyyy-mm-dd")
    val date = new Date(long)
    dateformat.format(date)
  }

  private def getDateDiff(diff: Long, base: Long, timeUnit: TimeUnit): Long = {
    val result = Math.abs(base - diff)
    timeUnit.convert(result, TimeUnit.MILLISECONDS)
  }
}
