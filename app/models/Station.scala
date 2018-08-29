package models

case class Station(id: Option[Int], name: String, location: String)

case class StationResult(id: Int, name: String, location: String, available: Int)

case class StationQuery(name: Option[String], minAvailable: Option[Int], maxAvailable: Option[Int], page: Int, pageSize: Int = 2)

case class LoginStation(station_id: Int)

case class Stations(stations: Seq[Station])