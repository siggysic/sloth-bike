package controllers.api

import java.sql.Timestamp
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import models._
import play.api.mvc.{AbstractController, Action, ControllerComponents, Result}
import repositories.{BikeRepository, BikeStatusRepository, HistoryRepository}
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonDSL._
import play.api.mvc.{AbstractController, Action, ControllerComponents, Result}
import repositories.{BikeRepository, BikeStatusRepository, HistoryRepository, PaymentRepository}
import utils.ClaimSet
import utils.Helpers.Authentication.decode
import utils.Helpers.Calculate
import utils.Helpers.EitherHelper.ExtractEitherT

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class BikeController @Inject()(cc: ControllerComponents, bikeRepository: BikeRepository, statusRepository: BikeStatusRepository,
                               historyRepository: HistoryRepository, paymentRepository: PaymentRepository)
                              (implicit ec: ExecutionContext) extends Response {
  import utils.Helpers.JsonHelper._
  import utils.Helpers.Authentication._
  import utils.Helpers.EitherHelper.CatchDatabaseExpAPI

  def getBikeTotal = authAsync { implicit req =>
    val bikeTotal: Future[Either[CustomException, BikeTotal]] = for {
      total <- bikeRepository.getBikeTotal()
    } yield Right(BikeTotal(total))

    response(bikeTotal)
  }

  def getBikesByBarcodeId(bid: String) = authAsync { implicit req =>
    val result: Future[Either[CustomException, Bike]] = for {
      bike <- bikeRepository.getBikeByKeyBarcode(bid)
    } yield bike match {
      case Right(Some(foundBike)) => Right(foundBike)
      case Right(None) => Left(NotFoundException("Barcode"))
      case Left(exp) => Left(exp)
    }

    response(result)
  }

  def getReturnBikeByBarcodeId(bid: String) = authAsync { implicit req =>
    val result: Future[Either[CustomException, BikeReturn]] = (
      for {
        bikeAndHistory <- bikeRepository.getBikeByKeyBarcodeWithHistory(bid).map {
          case Right(Nil) => Left(NotFoundException("Bike"))
          case other => other
        }.expToEitherT
      } yield {
        val bikeHistory = bikeAndHistory.head
        val arrivalDate = bikeHistory._2.borrowDate.map(_.toLocalDateTime.toLocalDate)
        val now = java.time.LocalDate.now
        val diff = java.time.Period.between(arrivalDate.getOrElse(now), now)
        val dateString = s"${diff.getYears} ปี ${diff.getMonths} เดือน ${diff.getDays} วัน"
        BikeReturn(Some(bikeHistory._1), dateString, Calculate.payment(diff.getDays))
      }
    ).value

    response(result)
  }

  def getBikesByStationId(sId: Int) = authAsync { implicit req =>
    val bikes = for {
        b <- bikeRepository.getBikesByStationId(sId)
      } yield b

    response(bikes)
  }

  def borrowBike = authAsync { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeBorrowedReq]

    println("Sad")
    val action: EitherT[Future, CustomException, JObject] =
      for {
        status <- {
          val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusByText("Borrowed") map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFoundException("Not found"))
              }
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }
        _ =     println("Sad2")

        b <- {
          val f = bikeRepository.updateByKeyBarcode(req.keyBarcode, status.id) map {
            case Right(i) => Right(i)
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }
        _ =     println("Sad3")

        bike <- {
          val f: Future[Either[CustomException, Bike]] = bikeRepository.getBikeByKeyBarcode(req.keyBarcode) map {
            case Right(data) =>
              data match {
                case Some(b) => Right(b)
                case None => Left(NotFoundException("Not found"))
              }
            case Left(_) => Left(DBException)
          }

          EitherT(f)
        }
        _ =     println("Sad4")

        history <- {
          val history = History(
            id = UUID.randomUUID().toString,
            studentId = Some(req.studentId),
            remark = None,
            borrowDate = Some(new Timestamp(System.currentTimeMillis())),
            returnDate = None,
            station = None,
            bikeId = bike.id,
            paymentId = None,
            statusId = status.id
          )
          val f: Future[Either[CustomException, Int]] = historyRepository.create(history) map Right.apply
          EitherT(f)
        }
      } yield JObject(Nil) ~ ("finished" -> true)

    response(action.value)
  }

  def returnBike = authAsync { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeReturnReq]
    val paymentReq: Option[PaymentReturn] = req.paymentReq
    val action =
      for {
        status <- {
          val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusByText("Available") map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFoundException("Not found"))
              }
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }
        _ = println("S")
        b <- {
          val f = bikeRepository.updateByKeyBarcode(req.keyBarcode, status.id) map {
            case Right(i) => Right(i)
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }
        _ = println("S1")

        p <- {
          val payment = Payment(
            UUID.randomUUID().toString,
            paymentReq.map(_.overtimeFine),
            paymentReq.map(_.defectFine),
            paymentReq.map(_.note)
          )
          val f = paymentRepository.createRecover(payment) map {
            case Right(id) => Right(payment)
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }
        _ = println("S2")

        h <- {
          val f: Future[Either[CustomException, Int]] = historyRepository.update(req.historyId, Some(p.id)) map {
            case Right(i) => Right(i)
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }
      } yield JObject(Nil) ~ ("finished" -> true)

    response(action.value)
  }

  def sendToRepair = authAsync { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeRepairReq]

    val action: EitherT[Future, CustomException, JObject] = for {
      status <- {
        val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusByText("OutOfOrder") map {
          case Right(data) =>
            data match {
              case Some(s) => Right(s)
              case None => Left(NotFoundException("Not found"))
            }

          case Left(_) => Left(DBException)
        }

        EitherT(f)
      }

      update <- {
        val f: Future[Either[CustomException, Int]] = bikeRepository.updateByKeyBarcode(req.keyBarcode, status.id) map {
          case Right(i) => Right(i)
          case Left(_) => Left(DBException)
        }
        EitherT(f)
      }

      bike <- {
        val f: Future[Either[CustomException, Bike]] = bikeRepository.getBikeByKeyBarcode(req.keyBarcode) map {
          case Right(data) =>
            data match {
              case Some(b) => Right(b)
              case None => Left(NotFoundException("Not found"))
            }
          case Left(_) => Left(DBException)
        }

        EitherT(f)
      }

      history <- {
        val history = History(
          id = UUID.randomUUID().toString,
          studentId = None,
          remark = None,
          borrowDate = Some(new Timestamp(System.currentTimeMillis())),
          returnDate = None,
          station = None,
          bikeId = bike.id,
          paymentId = None,
          statusId = status.id
        )
        val f: Future[Either[CustomException, Int]] = historyRepository.create(history) map Right.apply
        EitherT(f)
      }
    } yield JObject(Nil) ~ ("finished" -> true)

    response(action.value)
  }

  def repairReturnBike = authAsync { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeRepairReturnReq]
    val action =
      for {
        stationId <- {
          val result: Future[Either[CustomException, Int]] = decode(request.headers.get("Authorization").getOrElse("")) match {
            case Success(value) => ClaimSet(value) match {
              case Some(ok) => Future.successful(Right(ok.station_id.value))
              case None => Future.successful(Left(UnauthorizedException))
            }
            case Failure(_) => Future.successful(Left(UnauthorizedException))
          }
          EitherT(result)
        }

        _ = println(Console.GREEN + stationId + Console.RESET)

        status <- {
          val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusByText("Available") map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFoundException("Not found"))
              }
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }

        outOfOrder <- {
          val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusByText("OutOfOrder") map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFoundException("Not found"))
              }
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }

        b <- {
          val f = bikeRepository.updateStatusStationByKeyBarcode(req.keyBarcode, status.id, stationId) map {
            case Right(i) => Right(i)
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }

        bike <- {
          val f: Future[Either[CustomException, Bike]] = bikeRepository.getBikeByKeyBarcode(req.keyBarcode) map {
            case Right(data) =>
              data match {
                case Some(b) => Right(b)
                case None => Left(NotFoundException("Not found"))
              }
            case Left(_) => Left(DBException)
          }

          EitherT(f)
        }

        history <- {
          val f: Future[Either[CustomException, (History, BikeStatus)]] = historyRepository.getLastActionOfBike(bike.id, outOfOrder.id) map {
            case Right(data) =>
              data match {
                case Some(h) => Right(h)
                case None => Left(NotFoundException("Not found"))
              }
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }

        h <- {
          val f: Future[Either[CustomException, Int]] = historyRepository.update(history._1.id, None) map {
            case Right(i) => Right(i)
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }
      } yield JObject(Nil) ~ ("finished" -> true)

    response(action.value)
  }

}
