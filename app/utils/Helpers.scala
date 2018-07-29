package utils

import io.igl.jwt._
import net.liftweb.json.JValue
import net.liftweb.json.JsonAST.JString
import play.api.libs.json.{JsNumber, JsString, JsValue}
import net.liftweb.json.{DefaultFormats, JObject, JValue}
import play.api.libs.json.JsValue

import scala.util.Try

import scala.util.Try

case class ClaimSet(station_id: StationId, station_name: StationName, station_location: StationLocation) {
  def toSeq = Seq(station_id, station_name, station_location)
}

object ClaimSet {
  def apply(n: Int, u: String, e: String): ClaimSet = ClaimSet(StationId(n), StationName(u), StationLocation(e))
  def expected: Set[ClaimField] = Set(StationId)
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
  object JsonHelper {
    implicit private val format = DefaultFormats

    implicit class ExtendJsValue(json: JsValue) {
      def toJValue: JValue = Try(net.liftweb.json.parse(json.toString())).getOrElse(JObject(Nil))
    }
  }

  object Authentication {
    private val secret = "slothbike"
    private val algorithm = Algorithm.HS256

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
  }
}
