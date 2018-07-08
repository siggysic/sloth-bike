package repositories

import javax.inject.{Inject, Singleton}
import models.BikeStatus
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}


trait BikeStatusComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class BikeStatusTable(tag: Tag) extends Table[BikeStatus](tag, "bike_status") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")

    def * = (id, name) <> ((BikeStatus.apply _).tupled, BikeStatus.unapply)
  }
}

@Singleton()
class BikeStatusRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ex: ExecutionContext)
  extends BikeStatusComponent with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val bikeStatus = TableQuery[BikeStatusTable]

  def create(name: String): Future[Int] = {
    val action = bikeStatus.map(_.name).returning(bikeStatus.map(_.id)) += name
    db.run(action)
  }

  def getStatus: Future[Seq[BikeStatus]] = {
    val action = bikeStatus.result
    db.run(action)
  }
}
