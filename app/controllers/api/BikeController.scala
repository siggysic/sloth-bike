package controllers.api

import java.sql.Timestamp
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import models._
import net.liftweb.json.JsonAST.JObject
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import repositories.{BikeRepository, BikeStatusRepository, HistoryRepository, PaymentRepository}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class BikeController @Inject()(cc: ControllerComponents, bikeRepository: BikeRepository, statusRepository: BikeStatusRepository,
                               historyRepository: HistoryRepository, paymentRepository: PaymentRepository)
                              (implicit ec: ExecutionContext) extends AbstractController(cc) {
  import utils.Helpers.JsonHelper._

  def borrowBike = Action.async { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeBorrowedReq]
    val action =
      for {
        status <- {
          val f: Future[Either[Result, BikeStatus]] = statusRepository.getStatusByText("Borrowed") map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFound(""))
              }
            case Left(_) => Left(InternalServerError(""))
          }
          EitherT(f)
        }

        b <- {
          val f = bikeRepository.updateByKeyBarcode(req.keyBarcode, status.id) map {
            case Right(i) => Right(i)
            case Left(_) => Left(InternalServerError(""))
          }
          EitherT(f)
        }

        bike <- {
          val f: Future[Either[Result, Bike]] = bikeRepository.getBikeByKeyBarcode(req.keyBarcode) map {
            case Right(data) =>
              data match {
                case Some(b) => Right(b)
                case None => Left(NotFound(""))
              }
            case Left(_) => Left(InternalServerError(""))
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
          val f: Future[Either[Result, String]] = historyRepository.create(history) map Right.apply
          EitherT(f)
        }
      } yield Ok("")

    action.value.map {
      case Right(ok) => ok
      case Left(error) => error
    }
  }

  def returnBike = Action.async { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeReturnReq]
    val paymentReq: PaymentReturn = req.paymentReq
    val action =
      for {
        status <- {
          val f: Future[Either[Result, BikeStatus]] = statusRepository.getStatusByText("Available") map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFound(""))
              }
            case Left(_) => Left(InternalServerError(""))
          }
          EitherT(f)
        }

        b <- {
          val f = bikeRepository.updateByKeyBarcode(req.keyBarcode, status.id) map {
            case Right(i) => Right(i)
            case Left(_) => Left(InternalServerError(""))
          }
          EitherT(f)
        }

        p <- {
          val payment = Payment(
            UUID.randomUUID().toString,
            Some(paymentReq.overtimeFine),
            Some(paymentReq.defectFine),
            Some(paymentReq.note)
          )
          val f = paymentRepository.createRecover(payment) map {
            case Right(id) => Right(id)
            case Left(_) => Left(InternalServerError(""))
          }
          EitherT(f)
        }

        h <- {
          val f: Future[Either[Result, Int]] = historyRepository.update(req.historyId, Some(p)) map {
            case Right(i) => Right(i)
            case Left(_) => Left(InternalServerError(""))
          }
          EitherT(f)
        }
      } yield Ok("")

    action.value.map {
      case Right(ok) => ok
      case Left(error) => error
    }
  }

  def sendToRepair(keyBarcode: String) = Action.async { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeRepairReq]

    val a: EitherT[Future, Result, String] = for {
      status <- {
        val f: Future[Either[Result, BikeStatus]] = statusRepository.getStatusByText("OutOfOrder") map {
          case Right(data) =>
            data match {
              case Some(s) => Right(s)
              case None => Left(NotFound(""))
            }

          case Left(_) => Left(InternalServerError(""))
        }

        EitherT(f)
      }

      update <- {
        val f: Future[Either[Result, Int]] = bikeRepository.updateByKeyBarcode(keyBarcode, status.id) map {
          case Right(i) => Right(i)
          case Left(_) => Left(InternalServerError(""))
        }
        EitherT(f)
      }

      bike <- {
        val f: Future[Either[Result, Bike]] = bikeRepository.getBikeByKeyBarcode(keyBarcode) map {
          case Right(data) =>
            data match {
              case Some(b) => Right(b)
              case None => Left(NotFound(""))
            }
          case Left(_) => Left(InternalServerError(""))
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
        val f: Future[Either[Result, String]] = historyRepository.create(history) map Right.apply
        EitherT(f)
      }
    } yield ""
    Future.successful(Ok(""))
  }

  def repairReturnBike = Action.async { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeRepairReturnReq]
    val action =
      for {
        status <- {
          val f: Future[Either[Result, BikeStatus]] = statusRepository.getStatusByText("Available") map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFound(""))
              }
            case Left(_) => Left(InternalServerError(""))
          }
          EitherT(f)
        }

        outOfOrder <- {
          val f: Future[Either[Result, BikeStatus]] = statusRepository.getStatusByText("OutOfOrder") map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFound(""))
              }
            case Left(_) => Left(InternalServerError(""))
          }
          EitherT(f)
        }

        b <- {
          val f = bikeRepository.updateByKeyBarcode(req.keyBarcode, status.id) map {
            case Right(i) => Right(i)
            case Left(_) => Left(InternalServerError(""))
          }
          EitherT(f)
        }

        bike <- {
          val f: Future[Either[Result, Bike]] = bikeRepository.getBikeByKeyBarcode(req.keyBarcode) map {
            case Right(data) =>
              data match {
                case Some(b) => Right(b)
                case None => Left(NotFound(""))
              }
            case Left(_) => Left(InternalServerError(""))
          }

          EitherT(f)
        }

        history <- {
          val f: Future[Either[Result, (History, BikeStatus)]] = historyRepository.getLastActionOfBike(bike.id, outOfOrder.id) map {
            case Right(data) =>
              data match {
                case Some(h) => Right(h)
                case None => Left(NotFound(""))
              }
            case Left(_) => Left(InternalServerError(""))
          }
          EitherT(f)
        }

        h <- {
          val f: Future[Either[Result, Int]] = historyRepository.update(history._1.id, None) map {
            case Right(i) => Right(i)
            case Left(_) => Left(InternalServerError(""))
          }
          EitherT(f)
        }
      } yield Ok("")

    action.value.map {
      case Right(ok) => ok
      case Left(error) => error
    }
  }

}
