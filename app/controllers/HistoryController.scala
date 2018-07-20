package controllers

import javax.inject.Inject
import models.HistoryQuery
import play.api.mvc.{Action, Controller}
import repositories.HistoryRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class HistoryController @Inject()(historyRepository: HistoryRepository) extends Controller {
  def getHistories = Action {
    val h = historyRepository.getHistories(HistoryQuery(None, None, None, None, None, 0, 20))
    h onComplete {
      case Success(s) =>
        println(Console.RED + s + Console.RESET)

      case _ =>
        println(Console.RED + "Fail" + Console.RESET)

    }
    Ok("ASD")
  }
}
