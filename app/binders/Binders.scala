package binders

import java.net.URLEncoder

import models.BikeSearch
import play.api.data.Mapping
import play.api.mvc.QueryStringBindable

object Binders{
  implicit def queryStringBindable(implicit queryBinder: QueryStringBindable[Option[String]]) = new QueryStringBindable[BikeSearch] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, BikeSearch]] = {
      for {
        lotNo <- queryBinder.bind("lotNo", params)
        pieceNo <- queryBinder.bind("pieceNo", params)
        licencePlate <- queryBinder.bind("licensePlate", params)
        keyBarcode <- queryBinder.bind("keyBarcode", params)
        referenceId <- queryBinder.bind("referenceId", params)
        statusId <- queryBinder.bind("statusId", params)
      } yield {
        (lotNo, pieceNo, licencePlate, keyBarcode, referenceId, statusId) match {
          case (Right(lotNo), Right(pieceNo), Right(licencePlate), Right(keyBarcode), Right(referenceId), Right(statusId)) =>
            Right(BikeSearch(lotNo, pieceNo, licencePlate, keyBarcode, referenceId, statusId.map(_.toInt)))
          case _ => Left("Unable to bind an BikeSearch")
        }
      }
    }
    override def unbind(key: String, bikeSearch: BikeSearch): String = {
      queryBinder.unbind("lotNo", bikeSearch.lotNo) + "&" + queryBinder.unbind("pieceNo", bikeSearch.pieceNo) +
        "&" + queryBinder.unbind("licensePlate", bikeSearch.licensePlate) +
        "&" + queryBinder.unbind("keyBarcode", bikeSearch.keyBarcode) +
        "&" + queryBinder.unbind("referenceId", bikeSearch.referenceId) +
        "&" + queryBinder.unbind("statusId", bikeSearch.statusId.map(_.toString))

    }
  }

//  implicit def queryStringBindable[A](implicit mapping: Mapping[A]) = new QueryStringBindable[A] {
//    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, A]] = {
//      val data = for {
//        (k, ps) <- params
//        if k startsWith key
//        p <- ps.headOption
//      } yield (k.drop(key.length + 1), p)
//
//      if (data.isEmpty) None
//      else Some(mapping.bind(data.toMap).left.map(_ => "Unable to bind object for key '%s'".format(key)))
//    }
//    override def unbind(key: String, value: A): String = {
//      val map = mapping.unbind(value)
//      map.map { case (k, v) => key + "." + k + "=" + URLEncoder.encode(v, "utf-8") }.mkString("&")
//    }
//  }

}