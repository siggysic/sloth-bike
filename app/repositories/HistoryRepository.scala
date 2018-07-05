package repositories

import java.sql.Timestamp
import java.util.UUID

import javax.inject.{Inject, Singleton}
import models.{Bike, BikeStatus, History}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class HistoryRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends BikeComponent
    with BikeStatusComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private class Histories(tag: Tag) extends Table[History](tag, "histories") {
    def id = column[UUID]("Id", O.PrimaryKey)
    def fine = column[Int]("Fine")
    def remark = column[String]("Remark")
    def borrowDate = column[Timestamp]("BorrowDate")
    def returnDate = column[Timestamp]("ReturnDate")
    def bikeId = column[UUID]("BikeId")
    def statusId = column[Int]("StatusId")
    def * =
      (id, fine, remark, borrowDate, returnDate, bikeId, statusId) <> ((History.apply _).tupled, History.unapply)

    def bike = foreignKey("Bike", bikeId, TableQuery[Bikes])(_.id, onUpdate = ForeignKeyAction.Restrict)
    def status = foreignKey("Status", statusId, TableQuery[BikeStatusTable])(_.id, onUpdate = ForeignKeyAction.Restrict)
  }

  private val history = TableQuery[Histories]
  private val bike = TableQuery[Bikes]
  private val status = TableQuery[BikeStatusTable]

  def getHistories: Future[Seq[(History, Option[Bike], Option[BikeStatus])]] = {
    val queryBase = for {
      (h, b) <- history joinLeft bike on(_.bikeId === _.id)
      (_, s) <- history joinLeft status on(_.statusId === _.id)
    } yield (h, b, s)

    val action = for {
      query <- queryBase.result
    } yield query

    db.run(action)
  }
}
