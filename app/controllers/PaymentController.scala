package controllers

import javax.inject.Inject
import models.FullPayment
import play.api.mvc.{Action, Controller}
import repositories.PaymentRepository

import scala.concurrent.ExecutionContext.Implicits.global

class PaymentController @Inject()(paymentRepository: PaymentRepository)(implicit assets: AssetsFinder) extends Controller {
  def getPayments() = Action.async {

    paymentRepository.getFullPayment("","","").map { data =>
      Ok(views.html.payments(data.map(record => FullPayment(record._1.getOrElse(""), record._2, record._3,
          record._4, record._5, record._6, record._7, record._8))))
    }
  }
}