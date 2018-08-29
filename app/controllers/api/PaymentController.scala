package controllers.api

import java.util.UUID

import cats.data.EitherT
import javax.inject.Inject
import models._
import net.liftweb.json._
import play.api.mvc.{Action, ControllerComponents}
import repositories.{HistoryRepository, PaymentRepository, StudentRepository}
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try




class PaymentController @Inject()(cc: ControllerComponents, historyRepo: HistoryRepository, paymentRepo: PaymentRepository, studentRepository: StudentRepository)(implicit ec: ExecutionContext)
  extends Response {
  import utils.Helpers.JsonHelper._

  def getPayment(id: String) = Action.async {
    val resp = for {
      history <- {
        val result = historyRepo.getHistoryById(id).map {
          case Right(h) =>
            h match {
              case Some(data) =>
                if (Seq(data.studentId, data.paymentId).forall(_.isDefined))
                  Right(data)
                else
                  Left(models.NotFoundException("Not Found"))

              case None => Left(models.NotFoundException("Not Found"))
            }
          case Left(_) => Left(DBException)
        }
        EitherT(result)
      }
      payment <- {
        val result = paymentRepo.getPaymentById(history.paymentId.get) map {
          case Right(payments) =>
            val id = payments.find(_.parentId.isEmpty).map(_.id).getOrElse("")
            val defect = payments.foldLeft(0) { (acc, p) =>
              acc + p.defectFine.getOrElse(0)
            }
            val overtime = payments.foldLeft(0) { (acc, p) =>
              acc + p.overtimeFine.getOrElse(0)
            }
            val note= payments.flatMap(_.note).mkString(",")
            Right(PaymentResp(id, overtime, defect, note))

          case Left(_) => Left(DBException)
        }
        EitherT(result)
      }

      student <- {
        val result = studentRepository.getStudentById(history.studentId.get) map {
          case Right(student) =>
            student match {
              case Some(s) => Right(s)
              case None => Left(models.NotFoundException("Not Found"))
            }
          case Left(_) => Left(DBException)
        }
        EitherT(result)
      }
    } yield ModalResponse(student, payment, history)

    response(resp.value)
  }

  def createTransaction = Action.async { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    body.extractOpt[PaymentReq] match {
      case Some(p) =>
        val pay = Payment(id = UUID.randomUUID().toString, overtimeFine = None, defectFine = p.fine, note = p.note, parentId = p.parentId)
        pay.parentId match {
          case Some(_) =>
            paymentRepo.createRecover(pay).map { _ =>
              Ok("")
            }
          case None => Future.successful(BadRequest(""))
        }

      case None =>
        Future.successful(BadRequest(""))
    }
  }

}
