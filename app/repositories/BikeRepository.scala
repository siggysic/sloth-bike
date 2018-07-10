package repositories

import java.sql.Timestamp

import javax.inject.{Inject, Singleton}
import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait BikeComponent extends BikeStatusComponent with StationComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._
  class Bikes(tag: Tag) extends Table[Bike](tag, "bikes") {
    def id = column[String]("Id", O.PrimaryKey)
    def pieceNo = column[String]("PieceNo")
    def keyBarcode = column[Option[String]]("KeyBarcode")
    def referenceId = column[String]("ReferenceId")
    def lotNo = column[String]("LotNo")
    def licensePlate = column[String]("LicensePlate")
    def remark = column[Option[String]]("Remark")
    def detail = column[Option[String]]("Detail")
    def arrivalDate = column[Timestamp]("ArrivalDate")
    def createdAt = column[Timestamp]("CreatedAt")
    def updatedAt = column[Timestamp]("UpdatedAt")
    def statusId = column[Int]("StatusId")
    def stationId = column[Int]("StationId")

    def * =
      (id, keyBarcode, referenceId, licensePlate, lotNo, remark, detail, arrivalDate, pieceNo, createdAt, updatedAt, statusId, stationId) <> ((Bike.apply _).tupled, Bike.unapply)

    def status = foreignKey("Status", statusId, TableQuery[BikeStatusTable])(_.id, onUpdate = ForeignKeyAction.Cascade)
    def station = foreignKey("Station", statusId, TableQuery[Stations])(_.id, onUpdate = ForeignKeyAction.Cascade)
  }
}

@Singleton()
class BikeRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ex: ExecutionContext)
  extends BikeComponent with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val bike = TableQuery[Bikes]
  private val status = TableQuery[BikeStatusTable]
  private val station = TableQuery[Stations]

  def create(newBike: Bike): Future[Int] = {
    val action = bike += newBike
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

  def getBikesRelational(query: BikeQuery): Future[Seq[(Int, Bike, BikeStatus, Station)]] = {
    val total = bike.length
    val validateStatusId = query.statusId match {
      case Some(statusId) =>
        bike.filter(_.statusId === statusId)
      case None => bike
    }

    val offset = (query.page - 1) * query.pageSize
    val bikePagination = validateStatusId.drop(offset).take(query.pageSize)

    val action =  for {
      ((b, bs), s) <- bikePagination join status on (_.statusId === _.id) join station on (_._1.stationId === _.id)
    } yield (total, b, bs, s)

    db.run(action.result)
  }

//  def searchBikes(bikeSearch: BikeSearch, bikeQuery: BikeQuery) = {
//
//    val convert = (opt: Option[Any]) => opt.map(o => s"%$o%").getOrElse("%%")
//
//    val action = bike
//      .filter(_.pieceNo like(convert(bikeSearch.pieceNo)))
//      .filter(_.lotNo like(convert(bikeSearch.lotNo)))
//      .filter(_.licensePlate like(convert(bikeSearch.licensePlate)))
//      .filter(_.keyBarcode like(convert(bikeSearch.keyBarcode)))
//      .filter(_.referenceId like(convert(bikeSearch.referenceId)))
//
//    if(bikeSearch.statusId.nonEmpty) db.run(action.filter(_.statusId === bikeSearch.statusId.getOrElse("")).result)
//
//    db.run(action.result)
//  }
}
