package controllers

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date

import javax.inject.Inject
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, AnyContent, Controller, Request}
import repositories.HistoryRepository

import scala.concurrent.ExecutionContext.Implicits.global

class HistoryController @Inject()(historyRepository: HistoryRepository)(implicit assetsFinder: AssetsFinder) extends Controller {
  val queryForm: Form[BorrowStatisticTableForm] = Form(
    mapping(
      "startDate" -> optional(text),
      "endDate" -> optional(text),
      "page" -> default(number(min = 1), 1),
      "pageSize" -> default(number, 10)
    )
    (BorrowStatisticTableForm.apply)
    (BorrowStatisticTableForm.unapply)
  )

  def viewNumberOfUsageTable(startDate: Option[Long] = None, endDate: Option[Long] = None,page: Int = 1, size: Int = 10) =
    Action.async { implicit request =>
      val query = BorrowStatisticTableQuery(
        startDate.map(new Timestamp(_)),
        endDate.map(new Timestamp(_)),
        PageSize(page, size))

      val currentForm = queryForm.fill(BorrowStatisticTableForm
        (startDate.map(convertLongtoDateString), endDate.map(convertLongtoDateString), page, size)
      )

      historyRepository.getBorrowStatistic(query) map {
        case Right(data) =>
          Ok(views.html.borrowStatisticTable(data._2.map(d => BorrowStatisticTable(d._1, d._2)), data._1, currentForm))
        case Left(_) => BadRequest(views.html.exception("Database exception."))
      }
    }

  def filterViewUsageTableAction = Action { implicit request: Request[AnyContent] =>

    val failureFn = { formWithError: Form[BorrowStatisticTableForm] =>
      val startDate = formWithError.data.get("startDate").flatMap(convertDateToLong)
      val endDate = formWithError.data.get("endDate").flatMap(convertDateToLong)
      val page = formWithError.data.get("page").map(_.toInt).getOrElse(1)
      val size = formWithError.data.get("size").map(_.toInt).getOrElse(10)
      Redirect(routes.HistoryController.viewNumberOfUsageTable(startDate, endDate, page, size))
    }

    val successFn = { query: BorrowStatisticTableForm =>
      Redirect(routes.HistoryController.viewNumberOfUsageTable(
        query.startDate.flatMap(convertDateToLong),
        query.endDate.flatMap(convertDateToLong),
        query.page,
        query.size
      ))
    }

    queryForm.bindFromRequest().fold(failureFn, successFn)

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
}
