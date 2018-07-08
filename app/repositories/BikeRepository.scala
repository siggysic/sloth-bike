package repositories

import java.sql.Timestamp

import javax.inject.{Inject, Singleton}
import models.{Bike, BikeQuery}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait BikeComponent extends BikeStatusComponent with StationComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._
  class Bikes(tag: Tag) extends Table[Bike](tag, "bikes") {
    def id = column[String]("Id", O.PrimaryKey)
    def keyBarcode = column[Option[String]]("KeyBarcode")
    def referenceId = column[String]("ReferenceId")
    def lotNo = column[String]("LotNo")
    def licensePlate = column[String]("LicensePlate")
    def remark = column[Option[String]]("Remark")
    def detail = column[Option[String]]("Detail")
    def createdAt = column[Timestamp]("CreatedAt")
    def updatedAt = column[Timestamp]("UpdatedAt")
    def statusId = column[Int]("StatusId")
    def stationId = column[Int]("StationId")

    def * =
      (id, keyBarcode, referenceId, licensePlate, lotNo, remark, detail, createdAt, updatedAt, statusId) <> ((Bike.apply _).tupled, Bike.unapply)

    def status = foreignKey("Status", statusId, TableQuery[BikeStatusTable])(_.id, onUpdate = ForeignKeyAction.Cascade)
    def station = foreignKey("Station", statusId, TableQuery[Stations])(_.id, onUpdate = ForeignKeyAction.Cascade)
  }
}

@Singleton()
class BikeRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ex: ExecutionContext)
  extends BikeComponent with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val bike = TableQuery[Bikes]

  def create(newBike: Bike): Future[String] = {
    val action = bike.returning(bike.map(_.id)) += newBike
    db.run(action)
  }

  def getBikes(query: BikeQuery): Future[Seq[Bike]] = {
    val baseQuery = query.statusId match {
      case Some(statusId) =>
        bike.filter(_.statusId === statusId)
      case None => bike
    }

    val offset = (query.page - 1) * query.pageSize
    val action = baseQuery.drop(offset).take(query.pageSize).result
    db.run(action)
  }

  def getBike(id: String): Future[Option[Bike]] = {
    val action = bike.filter(_.id === id).result.headOption
    db.run(action)
  }
}
