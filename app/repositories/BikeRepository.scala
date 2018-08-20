package repositories

import java.sql.Timestamp
import java.util.UUID

import javax.inject.{Inject, Singleton}
import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.QueryBase
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.macros.whitebox

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
      (id, keyBarcode, referenceId, lotNo, licensePlate, remark, detail, arrivalDate, pieceNo, createdAt, updatedAt, statusId, stationId).shaped <> ((Bike.apply _).tupled, Bike.unapply)

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

  def createBulk(newBikes: Seq[Bike]): Future[Seq[Int]] = {
    val action = DBIO.sequence(newBikes.map(row => bike += row))
    db.run(action)
  }

  def update(updateBike: Bike): Future[Int] = {
    val action = bike.filter(_.id === updateBike.id).update(updateBike)
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

  def getBike(id: String): Future[Either[DBException.type, Option[(Bike, BikeStatus, Station)]]] = {
    val action = for {
      ((b, bs), s) <- bike.filter(_.id === id) join status on (_.statusId === _.id) join station on (_._1.stationId === _.id)
    } yield (b, bs, s)

    db.run(action.result.headOption).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

  def countBikeByStatusId(id: Int): Future[Either[DBException.type, Int]] = {
    val action = bike.filter(_.statusId === id)

    db.run(action.length.result).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

  def getBikeTotal(): Future[Int] = {
    val action = bike.length

    db.run(action.result)
  }

  def getBikesRelational(query: BikeQuery): Future[Either[DBException.type, Seq[(Int, Bike, BikeStatus, Station)]]] =
    getBikePagination(bike, query)

  def searchBikes(bikeSearch: BikeSearch, bikeQuery: BikeQuery) = {
    val convert = (opt: Option[Any]) => opt.map(o => s"%$o%").getOrElse("%%")

    val querySearch = bike
      .filter(_.pieceNo like(convert(bikeSearch.pieceNo)))
      .filter(_.lotNo like(convert(bikeSearch.lotNo)))
      .filter(_.licensePlate like(convert(bikeSearch.licensePlate)))
      .filter(_.keyBarcode like(convert(bikeSearch.keyBarcode)))
      .filter(_.referenceId like(convert(bikeSearch.referenceId)))

    val querySearchWithStatus = if(bikeSearch.statusId.nonEmpty) querySearch.filter(_.statusId === bikeSearch.statusId.get)
    else querySearch

    getBikePagination(querySearchWithStatus, bikeQuery)
  }

  private def getBikePagination(query: Query[Bikes, Bikes#TableElementType, Seq], bikeQuery: BikeQuery) = {

    val total = query.length

    val offset = (bikeQuery.page - 1) * bikeQuery.pageSize
    val bikePagination = query.drop(offset).take(bikeQuery.pageSize)

    val action =  for {
      ((b, bs), s) <- bikePagination join status on (_.statusId === _.id) join station on (_._1.stationId === _.id)
    } yield (total, b, bs, s)

    db.run(action.result).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

  def updateByKeyBarcode(keyBarcode: String, statusId: Int) = {
    val action = bike.filter(_.keyBarcode === Option.apply(keyBarcode)).map(_.statusId).update(statusId)
    db.run(action).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

  def getBikeByKeyBarcode(keyBarcode: String) = {
    val action = bike.filter(_.keyBarcode === Option.apply(keyBarcode)).result.headOption
    db.run(action).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }
}
