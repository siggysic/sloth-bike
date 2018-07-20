package repositories

import javax.inject.{Inject, Singleton}
import models.{Station, StationQuery}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}


trait StationComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._
  class Stations(tag: Tag) extends Table[Station](tag, "stations") {
    def id = column[Int]("Id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("Name")
    def location = column[String]("Location")

    def * = (id.?, name, location) <> ((Station.apply _).tupled, Station.unapply)
  }
}

@Singleton()
class StationRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends StationComponent
    with BikeComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val stations = TableQuery[Stations]
  private val bikes = TableQuery[Bikes]

  def create(newStation: Station) = {
    val action = stations.returning(stations.map(_.id)) += newStation
    db.run(action)
  }

  def getStations(query: StationQuery): Future[(Seq[(Int, String, String, Int)], Int)] = {
    val filterByName = query.name match {
      case Some(name) => stations.filter(_.name like s"%$name%")
      case None => stations
    }

    val joinBikes = filterByName.joinLeft(bikes).on(_.id === _.stationId)
      .groupBy(s => (s._1.id, s._1.name, s._1.location))
      .map({
        case (base, group) =>
          (base._1, base._2, base._3, group.map(_._2.map(_.id)).countDistinct)
      })

    val filterByAvailable = query.available match {
      case Some(available) => joinBikes.filter(_._4 === available)
      case None => joinBikes
    }

    val summary = filterByAvailable.drop((query.page - 1) * query.pageSize).take(query.pageSize)

    val totalAction = filterByAvailable.sortBy(_._1).length.result
    val action = summary.result
    db.run(action).flatMap(s => db.run(totalAction).map(total => (s, total)))
  }

  def getStations: Future[Seq[Station]] = {
    val action = stations.result
    db.run(action)
  }

  def getStation(id: Int): Future[Option[Station]] = {
    val action = stations.filter(_.id === id).result.headOption
    db.run(action)
  }
}
