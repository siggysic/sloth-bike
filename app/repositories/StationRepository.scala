package repositories

import javax.inject.{Inject, Singleton}
import models.Station
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext


trait StationComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._
  class Stations(tag: Tag) extends Table[Station](tag, "stations") {
    def id = column[Int]("Id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("Name")
    def location = column[String]("Location")
    def password = column[String]("Password")

    def * = (id, name, location, password.?) <> ((Station.apply _).tupled, Station.unapply)
  }
}

@Singleton()
class StationRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends StationComponent with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val stations = TableQuery[Stations]

  def create(name: String) = {
    val action = stations.map(_.name).returning(stations.map(_.id)) += name
    db.run(action)
  }

  def getStations(name: String) = {
    val action = stations.result
    db.run(action)
  }
}
