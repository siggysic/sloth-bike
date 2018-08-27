package controllers.api

import java.sql.Timestamp
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import models._
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonDSL._
import play.api.mvc.{Action, ControllerComponents}
import repositories.{BikeRepository, BikeStatusRepository, HistoryRepository, PaymentRepository}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class BikeController @Inject()(cc: ControllerComponents, bikeRepository: BikeRepository, statusRepository: BikeStatusRepository,
                               historyRepository: HistoryRepository, paymentRepository: PaymentRepository)
                              (implicit ec: ExecutionContext) extends Response {
  import utils.Helpers.JsonHelper._

  def borrowBike = Action.async { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeBorrowedReq]

    val action: EitherT[Future, CustomException, JObject] =
      for {
        status <- {
          val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusByText("Borrowed") map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(CustomException("Not found" :: Nil, 404))
              }
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }
          EitherT(f)
        }

        b <- {
          val f = bikeRepository.updateByKeyBarcode(req.keyBarcode, status.id) map {
            case Right(i) => Right(i)
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }
          EitherT(f)
        }

        bike <- {
          val f: Future[Either[CustomException, Bike]] = bikeRepository.getBikeByKeyBarcode(req.keyBarcode) map {
            case Right(data) =>
              data match {
                case Some(b) => Right(b)
                case None => Left(CustomException("Not found" :: Nil, 404))
              }
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }

          EitherT(f)
        }

        history <- {
          val history = History(
            id = UUID.randomUUID().toString,
            studentId = Some(req.studentId),
            remark = None,
            borrowDate = Some(new Timestamp(System.currentTimeMillis())),
            returnDate = None,
            station = Some(bike.stationId),
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

  def returnBike = Action.async { implicit request =>
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
                case None => Left(CustomException("Not found" :: Nil, 404))
              }
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }
          EitherT(f)
        }

        b <- {
          val f = bikeRepository.updateByKeyBarcode(req.keyBarcode, status.id) map {
            case Right(i) => Right(i)
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }
          EitherT(f)
        }

        p <- {
          val payment = Payment(
            UUID.randomUUID().toString,
            paymentReq.map(_.overtimeFine),
            paymentReq.map(_.defectFine),
            paymentReq.map(_.note)
          )
          val f = paymentRepository.createRecover(payment) map {
            case Right(id) => Right(payment)
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }
          EitherT(f)
        }

        h <- {
          val f: Future[Either[CustomException, Int]] = historyRepository.update(req.historyId, Some(p.id)) map {
            case Right(i) => Right(i)
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }
          EitherT(f)
        }
      } yield JObject(Nil) ~ ("finished" -> true)

    response(action.value)
  }

  def sendToRepair(keyBarcode: String) = Action.async { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeRepairReq]

    val action: EitherT[Future, CustomException, JObject] = for {
      status <- {
        val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusByText("OutOfOrder") map {
          case Right(data) =>
            data match {
              case Some(s) => Right(s)
              case None => Left(CustomException("Not found" :: Nil, 404))
            }

          case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
        }

        EitherT(f)
      }

      update <- {
        val f: Future[Either[CustomException, Int]] = bikeRepository.updateByKeyBarcode(keyBarcode, status.id) map {
          case Right(i) => Right(i)
          case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
        }
        EitherT(f)
      }

      bike <- {
        val f: Future[Either[CustomException, Bike]] = bikeRepository.getBikeByKeyBarcode(keyBarcode) map {
          case Right(data) =>
            data match {
              case Some(b) => Right(b)
              case None => Left(CustomException("Not found" :: Nil, 404))
            }
          case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
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

  def repairReturnBike = Action.async { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeRepairReturnReq]
    val action =
      for {
        status <- {
          val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusByText("Available") map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(CustomException("Not found" :: Nil, 404))
              }
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }
          EitherT(f)
        }

        outOfOrder <- {
          val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusByText("OutOfOrder") map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(CustomException("Not found" :: Nil, 404))
              }
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }
          EitherT(f)
        }

        b <- {
          val f = bikeRepository.updateByKeyBarcode(req.keyBarcode, status.id) map {
            case Right(i) => Right(i)
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }
          EitherT(f)
        }

        bike <- {
          val f: Future[Either[CustomException, Bike]] = bikeRepository.getBikeByKeyBarcode(req.keyBarcode) map {
            case Right(data) =>
              data match {
                case Some(b) => Right(b)
                case None => Left(CustomException("Not found" :: Nil, 404))
              }
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }

          EitherT(f)
        }

        history <- {
          val f: Future[Either[CustomException, (History, BikeStatus)]] = historyRepository.getLastActionOfBike(bike.id, outOfOrder.id) map {
            case Right(data) =>
              data match {
                case Some(h) => Right(h)
                case None => Left(CustomException("Not found" :: Nil, 404))
              }
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }
          EitherT(f)
        }

        h <- {
          val f: Future[Either[CustomException, Int]] = historyRepository.update(history._1.id, None) map {
            case Right(i) => Right(i)
            case Left(_) => Left(CustomException("DB Error" :: Nil, 500))
          }
          EitherT(f)
        }
      } yield JObject(Nil) ~ ("finished" -> true)

    response(action.value)
  }

}
