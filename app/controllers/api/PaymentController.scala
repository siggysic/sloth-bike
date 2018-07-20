package controllers.api

import java.sql.Timestamp

import javax.inject.Inject
import models.Payment
import net.liftweb.json.Serialization.write
import net.liftweb.json._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import repositories.PaymentRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PaymentController @Inject()(cc: ControllerComponents, paymentRepo: PaymentRepository)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  import utils.Helpers.JsonHelper._

  def getPayment(id: String) = Action.async {
    val a = new Timestamp(System.currentTimeMillis())
    val json = write(Payment("ASDSAD", Some(20), Some(30), None, a, a, None))
    Future.successful(Ok(Json.parse(json)))
  }

  def createTransaction = Action.async { implicit request =>
    val body = Try(request.body.asJson.get.toJValue).getOrElse(JObject(Nil))
    val payment: Payment = body.extract[Payment]
    paymentRepo.create(payment).map { _ =>
      Ok("")
    }
    payment.parentId match {
      case Some(_) => Future.successful(Ok(""))
      case None => Future.successful(BadRequest(""))
    }
  }

}
