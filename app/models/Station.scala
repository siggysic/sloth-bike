package models

case class Station(id: Option[Int], name: String, location: String)

case class StationResult(id: Int, name: String, location: String, available: Int)

case class StationQuery(name: Option[String], availableAsset: Option[String], page: Int, pageSize: Int = 10)

case class LoginStation(station_id: Int)

case class Stations(stations: Seq[Station])