package repositories

import java.sql.Timestamp

import javax.inject.{Inject, Singleton}
import models.Bike
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait BikeComponent extends BikeStatusComponent with StationComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._
  class Bikes(tag: Tag) extends Table[Bike](tag, "bikes") {
    def id = column[String]("Id", O.PrimaryKey)
    def keyBarcode = column[String]("KeyBarcode")
    def referenceId = column[String]("ReferenceId")
    def licensePlate = column[String]("LicensePlate")
    def remark = column[String]("Remark")
    def detail = column[String]("Detail")
    def createdAt = column[Timestamp]("CreatedAt")
    def updatedAt = column[Timestamp]("UpdatedAt")
    def statusId = column[Int]("StatusId")
    def stationId = column[Int]("StationId")

    def * =
      (id, keyBarcode, referenceId, licensePlate, remark, detail, createdAt, updatedAt, statusId) <> ((Bike.apply _).tupled, Bike.unapply)
    def status = foreignKey("Status", statusId, TableQuery[BikeStatusTable])(_.id, onUpdate = ForeignKeyAction.Restrict)
    def station = foreignKey("Station", statusId, TableQuery[Stations])(_.id, onUpdate = ForeignKeyAction.Restrict)
  }
}

@Singleton()
class BikeRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ex: ExecutionContext)
  extends BikeComponent with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val bike = TableQuery[Bikes]

  def create(keyBarcode: String, remark: String): Future[String] = {
    val action = bike.map(p => (p.keyBarcode, p.remark)).returning(bike.map(_.id)) += (keyBarcode, remark)
    db.run(action)
  }
}
