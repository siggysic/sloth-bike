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
import repositories._
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonDSL._
import play.api.mvc.{Action, ControllerComponents}
import utils.ClaimSet
import utils.Helpers.Authentication.decode
import utils.Helpers.Calculate
import utils.Helpers.EitherHelper.ExtractEitherT

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class BikeController @Inject()(cc: ControllerComponents, bikeRepository: BikeRepository, statusRepository: BikeStatusRepository,
                               historyRepository: HistoryRepository, paymentRepository: PaymentRepository,
                               studentRepository: StudentRepository, facultyRepository: FacultyRepository)
                              (implicit ec: ExecutionContext) extends Response {
  import utils.Helpers.JsonHelper._
  import utils.Helpers.Authentication._
  import utils.Helpers.EitherHelper.CatchDatabaseExpAPI

  def getBikeTotal(status: Option[Int]) = authAsync { implicit req =>
    val bikeTotal: Future[Either[CustomException, BikeTotal]] = (
      for {
        total <- bikeRepository.getBikeTotal(status).expToEitherT
      } yield BikeTotal(total)
    ).value
    response(bikeTotal)
  }

  def getBikesByBarcodeId(bid: String) = authAsync { implicit req =>
    val result: Future[Either[CustomException, Bike]] = for {
      bike <- bikeRepository.getBikeByKeyBarcode(bid)
    } yield bike match {
      case Right(Some(foundBike)) => Right(foundBike)
      case Right(None) => Left(NotFoundThaiLangException("กุญแจ"))
      case Left(exp) => Left(exp)
    }

    response(result)
  }

  def getReturnBikeByBarcodeId(bid: String) = authAsync { implicit req =>
    val result: Future[Either[CustomException, BikeReturn]] = (
      for {
        bike <- bikeRepository.getBikeByKeyBarcode(bid).map {
          case Right(Some(value)) => Right(value)
          case Right(None) => Left(NotFoundThaiLangException("จักรยาน"))
          case Left(exp) => Left(exp)
        }.expToEitherT

        history <- historyRepository.getLastActionOfBike(bike.id, 2).map {
          case Right(Some(value)) => Right(value)
          case Right(None) => Left(NotFoundThaiLangException("ประวัติจักรยาน"))
          case Left(exp) => Left(exp)
        }.expToEitherT

        student <- studentRepository.getStudentById(history._1.studentId.getOrElse("")).map {
          case Right(Some(value)) => Right(value)
          case Right(None) => Left(NotFoundThaiLangException("นักศึกษา"))
          case Left(exp) => Left(exp)
        }.expToEitherT

        faculty <- facultyRepository.getFaculty(Try(student.major.getOrElse("0").toInt).getOrElse(0)).map {
          case Right(Some(value)) => Right(value)
          case Right(None) => Right(Faculty(Some(0), "", ""))
          case Left(exp) => Left(exp)
        }.expToEitherT
      } yield {
        val arrivalDate = history._1.borrowDate.map(_.toLocalDateTime.toLocalDate)
        val now = java.time.LocalDate.now
        val diff = java.time.Period.between(arrivalDate.getOrElse(now), now)
        val dateString = s"${diff.getYears} ปี ${diff.getMonths} เดือน ${diff.getDays} วัน"

        BikeReturn(bike, student.toStudentWithFaculty(faculty), history._1, dateString, Calculate.payment(diff.getDays))
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

    val action: EitherT[Future, CustomException, JObject] =
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

        validateBike <- {
          val f: Future[Either[CustomException, Bike]] = bikeRepository.getBikesByStationIdAndKeyBarcode(req.keyBarcode, stationId) map {
            case Right(data) =>
              data match {
                case Some(b) => Right(b)
                case None => Left(NotFoundThaiLangException("จักรยาน"))
              }
            case Left(_) => Left(DBException)
          }

          EitherT(f)
        }

        validateStudent <- {
          val f: Future[Either[CustomException, Option[(History, BikeStatus)]]] = historyRepository.getLastActionOfStudent(req.studentId) map {
            case Right(data) =>
              data match {
                case Some(b) => Left(BadRequestException(s"${req.studentId} ไม่สามารถยืมจักรยานได้"))
                case None => Right(data)
              }
            case Left(_) => Left(DBException)
          }

          EitherT(f)
        }

        validateStudentStatus <- {
          val f: Future[Either[CustomException, Student]] = studentRepository.getStudentById(req.studentId) map {
            case Right(data) =>
              data match {
                case Some(b) if b.status.toLowerCase == "active" => Right(b)
                case _ => Left(BadRequestException(s"${req.studentId} สถานะไม่พร้อมใช้งาน"))
              }
            case Left(_) => Left(DBException)
          }

          EitherT(f)
        }

        status <- {
          val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusById(2) map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFoundThaiLangException("สถานะ"))
              }
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }

        b <- {
          val f = bikeRepository.updateByKeyBarcode(req.keyBarcode, status.id) map {
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
                case None => Left(NotFoundThaiLangException("จักรยาน"))
              }
            case Left(_) => Left(DBException)
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

  def returnBike = authAsync { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val req = body.extract[BikeReturnReq]
    val paymentReq: Option[PaymentReturn] = req.payment
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

        validateBike <- {
          val f: Future[Either[CustomException, Bike]] = bikeRepository.getBikeByKeyBarcode(req.keyBarcode) map {
            case Right(data) =>
              data match {
                case Some(b) => b.statusId == 2 match {
                  case true => Right(b)
                  case false => Left(BadRequestException("จักรยานไม่สามารถยืมได้"))
                }
                case None => Left(NotFoundThaiLangException("จักรยาน"))
              }
            case Left(_) => Left(DBException)
          }

          EitherT(f)
        }

        validatePayment <- {
          val f: Future[Either[CustomException, (History, BikeStatus)]] = historyRepository.getLastActionOfBike(validateBike.id, 2) map {
            case Right(data) =>
              data match {
                case Some(b) => b._1.id == req.historyId match {
                  case true => Right(b)
                  case false => Left(BadRequestException("ประวัติจักรยานไม่ตรงกัน"))
                }
                case None => Left(NotFoundThaiLangException("จักรยาน"))
              }
            case Left(_) => Left(DBException)
          }

          EitherT(f)
        }

        status <- {
          val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusById(1) map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFoundThaiLangException("สถานะ"))
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

      validateBike <- {
        val f: Future[Either[CustomException, Bike]] = bikeRepository.getBikeByKeyBarcode(req.keyBarcode) map {
          case Right(data) =>
            data match {
              case Some(b) => b.statusId == 1 match {
                case true => Right(b)
                case false => Left(BadRequestException("จักรยานไม่พร้อมใช้งาน"))
              }
              case None => Left(NotFoundThaiLangException("จักรยาน"))
            }
          case Left(_) => Left(DBException)
        }

        EitherT(f)
      }

      status <- {
        val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusById(3) map {
          case Right(data) =>
            data match {
              case Some(s) => Right(s)
              case None => Left(NotFoundThaiLangException("สถานะ"))
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

      history <- {
        val history = History(
          id = UUID.randomUUID().toString,
          studentId = None,
          remark = None,
          borrowDate = Some(new Timestamp(System.currentTimeMillis())),
          returnDate = None,
          station = None,
          bikeId = validateBike.id,
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

        validateBike <- {
          val f: Future[Either[CustomException, Bike]] = bikeRepository.getBikeByKeyBarcode(req.keyBarcode) map {
            case Right(data) =>
              data match {
                case Some(b) => b.statusId == 3 match {
                  case true => Right(b)
                  case false => Left(BadRequestException("สถานะของจักรยานไม่สามารถคืนได้"))
                }
                case None => Left(NotFoundThaiLangException("จักรยาน"))
              }
            case Left(_) => Left(DBException)
          }

          EitherT(f)
        }

        history <- {
          val f: Future[Either[CustomException, (History, BikeStatus)]] = historyRepository.getLastActionOfBike(validateBike.id, 3) map {
            case Right(data) =>
              data match {
                case Some(h) => Right(h)
                case None => Left(NotFoundThaiLangException("ประวัติจักรยาน"))
              }
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }

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

        status <- {
          val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusById(1) map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFoundThaiLangException("สถานะ"))
              }
            case Left(_) => Left(DBException)
          }
          EitherT(f)
        }

        outOfOrder <- {
          val f: Future[Either[CustomException, BikeStatus]] = statusRepository.getStatusById(3) map {
            case Right(data) =>
              data match {
                case Some(status) => Right(status)
                case None => Left(NotFoundThaiLangException("สถานะ"))
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
