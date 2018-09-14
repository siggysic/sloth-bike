package controllers

import java.sql.Timestamp
import java.time.temporal.ChronoUnit
import java.util.{Calendar, Date}

import cats.implicits._
import javax.inject.{Inject, Singleton}
import models.{GraphLogin, TimeUsageReport, TimeUsageReportField}
import play.api.data.Form
import play.api.mvc._
import play.api.data.Forms._
import repositories._
import utils.Helpers.EitherHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import utils.Helpers.EitherHelper.{CatchDatabaseExp, ExtractEitherT}

import scala.collection.immutable

@Singleton()
class ReportController @Inject()(cc: ControllerComponents, bikeRepo: BikeRepository,
                                 bikeStatusRepository: BikeStatusRepository, historyRepository: HistoryRepository)
                                (implicit assetsFinder: AssetsFinder)
  extends AbstractController(cc) {

  val timeUsageForm: Form[TimeUsageReport] = Form (
    mapping(
      "startDate" -> optional(date),
      "endDate" -> optional(date)
    )(TimeUsageReport.apply)(TimeUsageReport.unapply)
  )

  def viewReport = Action.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.reports()))
  }

  def viewAssetReport = Action.async { implicit request: Request[AnyContent] =>
    val c = Calendar.getInstance
    c.setTime(new Date())
    c.add(Calendar.DATE, 1)
    (
      for {
        status <- bikeStatusRepository.getStatus.map { es =>
          es.right.map { rs =>
            (rs.find(_.id == 1), rs.find(_.id == 3)) match {
              case (Some(avail), Some(out)) => (Some(avail), Some(out))
              case other => other
            }
          }
        }.dbExpToEitherT

        bikeAvail <- bikeRepo.countBikeByStatusId(status._1.map(_.id).getOrElse(0)).dbExpToEitherT
        bikeOut <- bikeRepo.countBikeByStatusId(status._2.map(_.id).getOrElse(0)).dbExpToEitherT
        borrowOneDay <- bikeRepo.getTotalBikeBorrow(2, new Timestamp(c.getTime.getTime)).dbExpToEitherT
        borrowMoreThanOne <- bikeRepo.getTotalBikeBorrow(2, new Timestamp(c.getTime.getTime), false).dbExpToEitherT
      } yield Ok(views.html.assetReport(GraphLogin(bikeAvail, bikeOut, borrowOneDay, borrowMoreThanOne)))
    ).extract
  }

  def viewTimeUsageReport = Action.async { implicit request: Request[AnyContent] =>
    val fields = TimeUsageReportField()
    (
      for {
        histories <- historyRepository.getCompleteHistories.dbExpToEitherT
      } yield {
        val hourHistory: Seq[(String, Long)] = histories.flatMap { h =>
          (h.borrowDate, h.returnDate) match {
            case (Some(b), Some(r)) =>
              val bld =  b.toLocalDateTime
              val rld =  r.toLocalDateTime
              val hour = ChronoUnit.HOURS.between(bld, rld)
              Some(period(hour))
            case _ => None
          }
        }
        val result = hourHistory.groupBy(_._1).map(h => (h._1, h._2.length))
        Ok(views.html.timeUsageReport(timeUsageForm, fields, result))
      }
    ).extract
  }

  def searchTimeUsageReport = Action.async { implicit request: Request[AnyContent] =>
    val fields = TimeUsageReportField()
    val errorFunction = { error: Form[TimeUsageReport] =>
      (
        for {
          histories <- historyRepository.getCompleteHistories.dbExpToEitherT
        } yield {
          val hourHistory: Seq[(String, Long)] = histories.flatMap { h =>
            (h.borrowDate, h.returnDate) match {
              case (Some(b), Some(r)) =>
                val bld =  b.toLocalDateTime
                val rld =  r.toLocalDateTime
                val hour = ChronoUnit.HOURS.between(bld, rld)
                Some(period(hour))
              case _ => None
            }
          }
          val result = hourHistory.groupBy(_._1).map(h => (h._1, h._2.length))
          Ok(views.html.timeUsageReport(error, fields, result))
        }
      ).extract
    }

    val successFunction = { data: TimeUsageReport =>
      (
        for {
          histories <- historyRepository.getHistoriesByDate(
            new Timestamp(data.startDate.getOrElse(new java.util.Date()).getTime),
            new Timestamp(data.endDate.getOrElse(new java.util.Date()).getTime)
          ).dbExpToEitherT
        } yield {
          val hourHistory: Seq[(String, Long)] = histories.flatMap { h =>
            (h.borrowDate, h.returnDate) match {
              case (Some(b), Some(r)) =>
                val bld =  b.toLocalDateTime
                val rld =  r.toLocalDateTime
                val hour = ChronoUnit.HOURS.between(bld, rld)
                Some(period(hour))
              case _ => None
            }
          }
          val result = hourHistory.groupBy(_._1).map(h => (h._1, h._2.length))
          Ok(views.html.timeUsageReport(timeUsageForm.fill(data), fields, result))
        }
      ).extract
    }

    val formValidationResult: Form[TimeUsageReport] = timeUsageForm.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }

  private def period(i: Long): (String, Long) = {
    if(i <= 3) {
      ("0-3 ชม.", i)
    }else if(i <= 6) {
      ("3-6 ชม.", i)
    }else if(i <= 9) {
      ("6-9 ชม.", i)
    }else if(i <= 24) {
      ("9-24 ชม.", i)
    }else {
      ("24++", i)
    }
  }

}
