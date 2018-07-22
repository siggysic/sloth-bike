package controllers.api

import java.sql.Timestamp
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import models.{Bike, BikeStatus, History, PaymentRequest, Payment, BikeReturnReq}
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import repositories.{BikeRepository, BikeStatusRepository, HistoryRepository}

import scala.concurrent.{ExecutionContext, Future}

class BikeController @Inject()(cc: ControllerComponents, bikeRepository: BikeRepository, statusRepository: BikeStatusRepository,
                               historyRepository: HistoryRepository)
                              (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def returnBike(keyBarcode: String, historyId: String, paymentReq: PaymentRequest) = Action.async {
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeReturnReq]
    val paymentReq = req.paymentReq
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
          val f: Future[Either[Result, Int]] = historyRepository.update(req.historyId, p) map {
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

  def sendToRepair(keyBarcode: String) = Action.async {
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

}
