package controllers.api

import java.util.concurrent.TimeUnit

import javax.inject.Inject
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import repositories.HistoryRepository
import response.SuccessResponse

import scala.concurrent.duration.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

class HistoryController @Inject()(cc: ControllerComponents, historyRepository: HistoryRepository)
                                 (implicit ec: ExecutionContext) extends AbstractController(cc) {
  implicit val format = DefaultFormats

  def getHistoryWithPayment(id: String) = Action.async {
    historyRepository.getHistoryWithPayment(id)
    Future.successful(Ok(""))
  }

  def getStudentFine = Action.async {
    import models.mappers.HistoryMapper._
    val fine = historyRepository.getLastActionOfStudent("").map {
      case Right(data) =>
        val result = data.map(_.toQueryClass)
        val borrowedDate = result.flatMap(_.borrowDate)
        val fine = borrowedDate.map(b => getDateDiff(b.getTime, System.currentTimeMillis(), TimeUnit.HOURS).toInt).getOrElse(0)
        FineResult(fine)
      case Left(_) =>
        FineResult(0)
    }

    fine.map(f => Ok(Json.parse(write(
      SuccessResponse(data = Json.parse(write(f)), statusCode = 200)
    ))))
  }

  private def getDateDiff(diff: Long, base: Long, timeUnit: TimeUnit): Long = {
    val result = Math.abs(base - diff)
    timeUnit.convert(result, TimeUnit.MILLISECONDS)
  }

  case class FineResult(fine: Int)
}
