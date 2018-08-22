package controllers

import java.sql.Timestamp
import java.util.Calendar

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import models._
import play.api.data.Form
import play.api.data.Forms.{date, mapping, optional, text}
import play.api.mvc._
import repositories.{AuthenticationRepository, BikeRepository, BikeStatusRepository, PaymentRepository}
import utils.Helpers.EitherHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.util.Date
import utils.Helpers.EitherHelper.{CatchDatabaseExp, ExtractEitherT}

class LoginController @Inject()(cc: ControllerComponents, bikeRepo: BikeRepository,
                                bikeStatusRepository: BikeStatusRepository,
                                authenticationRepository: AuthenticationRepository)
                               (implicit assets: AssetsFinder) extends AbstractController(cc) {

  val loginForm: Form[Login] = Form (
    mapping(
      "username" -> text.verifying(Contraints.validateText),
      "password" -> text.verifying(Contraints.validateText),
    )(Login.apply)(Login.unapply)
  )

  val fields = LoginFields()

  def viewLogin() = Action.async { implicit request: Request[AnyContent] =>
    val c = Calendar.getInstance
    c.setTime(new Date())
    c.add(Calendar.DATE, 1)
    (
      for {
        status <- bikeStatusRepository.getStatus.map { es =>
          es.right.map { rs =>
            (rs.find(_.status == "Available"), rs.find(_.status == "OutOfOrder")) match {
              case (Some(avail), Some(out)) => (Some(avail), Some(out))
              case other => other
            }
          }
        }.dbExpToEitherT

        bikeAvail <- bikeRepo.countBikeByStatusId(status._1.map(_.id).getOrElse(0)).dbExpToEitherT
        bikeOut <- bikeRepo.countBikeByStatusId(status._2.map(_.id).getOrElse(0)).dbExpToEitherT
        borrowOneDay <- bikeRepo.getTotalBikeBorrow("Borrowed", new Timestamp(c.getTime.getTime)).dbExpToEitherT
        borrowMoreThanOne <- bikeRepo.getTotalBikeBorrow("Borrowed", new Timestamp(c.getTime.getTime), false).dbExpToEitherT
      } yield Ok(views.html.login(loginForm, fields, GraphLogin(bikeAvail, bikeOut, borrowOneDay, borrowMoreThanOne)))
    ).extract
  }

  def doLogin() = Action.async { implicit request: Request[AnyContent] =>
    val errorFunction = { formWithErrors: Form[Login] =>
      (
        for {
          status <- bikeStatusRepository.getStatus.map { es =>
            es.right.map { rs =>
              (rs.find(_.status == "Available"), rs.find(_.status == "OutOfOrder")) match {
                case (Some(avail), Some(out)) => (Some(avail), Some(out))
                case other => other
              }
            }
          }.dbExpToEitherT

          bikeAvail <- bikeRepo.countBikeByStatusId(status._1.map(_.id).getOrElse(0)).dbExpToEitherT
          bikeOut <- bikeRepo.countBikeByStatusId(status._2.map(_.id).getOrElse(0)).dbExpToEitherT
        } yield BadRequest(views.html.login(formWithErrors, fields, GraphLogin(bikeAvail, bikeOut, 1, 1))).withNewSession
      ).extract
    }

    val successFunction = { data: Login =>
      (
        for {
          login <- authenticationRepository.login(data)
        } yield {
          Redirect("/assets").withSession(
            request.session + ("username" -> data.username)
          )
        }
      )
    }

    val formValidationResult: Form[Login] = loginForm.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }
}