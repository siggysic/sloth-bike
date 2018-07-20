package controllers

import javax.inject.Inject
import play.api.mvc.{Action, Controller}
import repositories.PaymentRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class PaymentController @Inject()(paymentRepository: PaymentRepository) extends Controller {
  def getPayments() = Action {
    val p = paymentRepository.getPayments("")
    p onComplete {
      case Success(s) =>
        println(Console.RED + s + Console.RESET)
      case _ =>
        println(Console.RED + "Fail" + Console.RESET)
    }
    Ok("Hi")
  }
}
