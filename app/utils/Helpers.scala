package utils

import cats.data.EitherT
import io.igl.jwt._
import models.{Response, UnauthorizedException}
import net.liftweb.json.{DefaultFormats, JObject, JValue}
import play.api.libs.json.{JsNumber, JsString, JsValue}
import play.api.mvc._
import models.DBException
import cats.implicits._
import controllers.AssetsFinder

import scala.concurrent.Future
import scala.util.{Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class ClaimSet(station_id: StationId, station_name: StationName, station_location: StationLocation) {
  def toSeq = Seq(station_id, station_name, station_location)
}

object ClaimSet {
  def apply(n: Int, u: String, e: String): ClaimSet = ClaimSet(StationId(n), StationName(u), StationLocation(e))
  def expected: Set[ClaimField] = Set(StationId, StationName, StationLocation)
}

case class StationId(value: Int) extends ClaimValue {
  override val field: ClaimField = StationId
  override val jsValue: JsValue = JsNumber(value)
}

object StationId extends ClaimField {
  override def attemptApply(value: JsValue): Option[ClaimValue] =
    value.asOpt[Int].map(apply)
  override val name: String = "station_id"
}

case class StationName(value: String) extends ClaimValue {
  override val field: ClaimField = StationName
  override val jsValue: JsValue = JsString(value)
}

object StationName extends ClaimField {
  override def attemptApply(value: JsValue): Option[ClaimValue] =
    value.asOpt[String].map(apply)
  override val name: String = "station_name"
}

case class StationLocation(value: String) extends ClaimValue {
  override val field: ClaimField = StationLocation
  override val jsValue: JsValue = JsString(value)
}

object StationLocation extends ClaimField {
  override def attemptApply(value: JsValue): Option[ClaimValue] =
    value.asOpt[String].map(apply)
  override val name: String = "station_location"
}

object Helpers {

  object EitherHelper extends Controller {
    implicit class CatchDatabaseExp[T](fe: Future[Either[DBException.type, T]])(implicit assetsFinder: AssetsFinder, request: Request[AnyContent]) {
      def dbExpToEitherT = {
        val fet = fe.map { e =>
          e match {
            case Right(value) => Right(value)
            case Left(_) => Left(InternalServerError(views.html.exception("Database exception.")))
          }
        }
        EitherT(fet)
      }
    }

    implicit class CatchDatabaseExpWithoutResult[T](fe: Future[Either[DBException.type, T]]) {
      def dbExpToEitherT = EitherT(fe)
    }

    implicit class ExtractEitherT(fr: EitherT[Future, Result, Result]) {
      def extract = fr.value.map {
        case Right(pass) => pass
        case Left(fail) => fail
      }
    }
  }

  object JsonHelper {
    implicit private val format = DefaultFormats

    implicit class ExtendJsValue(json: JsValue) {
      def toJValue: JValue = Try(net.liftweb.json.parse(json.toString())).getOrElse(JObject(Nil))
    }
  }

  object Authentication extends Response {
    private val secret = "slothbike"
    private val algorithm = Algorithm.HS256

    private def verifyJWT[W](block: Request[W] => Future[Result])(req: Request[W]): Future[Result] =
      decode(req.headers.get("Authorization").getOrElse("")) match {
        case Success(_) => block(req)
        case _ => response(Future.successful(Left(UnauthorizedException)))
      }

    def encode(cs: ClaimSet): String = new DecodedJwt(Seq(Alg(algorithm), Typ("JWT")), cs.toSeq).encodedAndSigned(secret)

    def decode(token: String): Try[Jwt] = {
      DecodedJwt.validateEncodedJwt(
        token,
        secret,
        algorithm,
        Set(Typ),
        ClaimSet.expected
      )
    }

    def authAsync(block: Request[AnyContent] => Future[Result]): Action[AnyContent] = Action.async(verifyJWT(block) _)
  }
}
