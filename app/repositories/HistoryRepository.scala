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
    with PaymentComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private class Histories(tag: Tag) extends Table[History](tag, "histories") {
    def id = column[UUID]("Id", O.PrimaryKey)
    def studentId = column[String]("studentId")
    def remark = column[Option[String]]("Remark")
    def borrowDate = column[Timestamp]("BorrowDate")
    def returnDate = column[Option[Timestamp]]("ReturnDate")
    def updatedAt = column[Timestamp]("UpdatedAt")
    def createdAt = column[Timestamp]("CreatedAt")
    def statusId = column[Int]("StatusId")
    def bikeId = column[String]("BikeId")
    def paymentId = column[Option[UUID]]("PaymentId")
    def * =
      (id, studentId, remark, borrowDate, returnDate, createdAt, updatedAt, statusId, bikeId, paymentId) <>
        ((History.apply _).tupled, History.unapply)

    def bike = foreignKey("Bike", bikeId, TableQuery[Bikes])(_.id, onUpdate = ForeignKeyAction.Cascade)
    def status = foreignKey("Status", statusId, TableQuery[BikeStatusTable])(_.id, onUpdate = ForeignKeyAction.Cascade)
    def payment = foreignKey("Payment", paymentId, TableQuery[Payments])(_.id.?, onUpdate = ForeignKeyAction.Cascade)
  }

  private val history = TableQuery[Histories]
  private val bike = TableQuery[Bikes]
  private val status = TableQuery[BikeStatusTable]
  private val payment = TableQuery[Payments]

  def getHistories: Future[Seq[(((History, Option[Bike]), Option[BikeStatus]), Option[(UUID, Option[Int])])]] = {
    val queryBase = history.joinLeft(bike).on(_.bikeId === _.id)
      .joinLeft(status).on(_._1.statusId === _.id)
      .joinLeft(
        payment.join(payment).on(_.id === _.parentId)
          .groupBy(_._1.id)
          .map {
            case (id, group) =>
              val baseOvertimeFine = group.map(_._1.overtimeFine.getOrElse(0)).max
              val baseDefectFine = group.map(_._1.defectFine.getOrElse(0)).max
              val subOvertimeFine =
                group.map(p => p._2.overtimeFine).sum.getOrElse(0)
              val subDefectFine =
                group.map(p => p._2.defectFine).sum.getOrElse(0)

              (id, baseOvertimeFine + subOvertimeFine + baseDefectFine + subDefectFine)
          }
      ).on(_._1._1.paymentId === _._1)

    val action = for {
      query <- queryBase.result
    } yield query

    db.run(action)
  }
}
