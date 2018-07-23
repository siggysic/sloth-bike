package repositories

import java.sql.Timestamp

import exceptions.DBException
import javax.inject.{Inject, Singleton}
import models.{History, Payment}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait PaymentComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class Payments(tag: Tag) extends Table[Payment](tag, "payments") {
    def id = column[String]("Id", O.PrimaryKey)
    def overtimeFine = column[Option[Int]]("OvertimeFine")
    def defectFine = column[Option[Int]]("DefectFine")
    def note = column[String]("Note")
    def updatedAt = column[Timestamp]("UpdatedAt")
    def createdAt = column[Timestamp]("CreatedAt")
    def parentId = column[Option[String]]("ParentId")

    def * =
      (id, overtimeFine, defectFine, note.?, createdAt, updatedAt, parentId) <> ((Payment.apply _).tupled, Payment.unapply)

    def payment =
      foreignKey("payment", parentId, TableQuery[Payments])(_.id.?, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
  }
}

@Singleton()
class PaymentRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile]
    with PaymentComponent
    with HistoryComponent
    with StudentComponent {

  import profile.api._

  private val payment = TableQuery[Payments]
  private val history = TableQuery[Histories]
  private val student = TableQuery[Students]

  def getPayments(studentId: String): Future[Seq[(String, Option[Int], Option[Int])]] = {
    val action =
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

          (id, baseOvertimeFine + subOvertimeFine, baseDefectFine + subDefectFine)
      }.result

    db.run(action)
  }

  def create(payment: Payment) = {
    val action = this.payment.returning(this.payment.map(_.id)) += payment
    db.run(action)
  }

  def getFullPayment(studentId: String, studentName: String, major: String) = {
    val action = payment
      .join(payment).on(_.id === _.parentId)
      .joinRight(history).on(_._1.id === _.paymentId)
      .join(student).on(_._2.studentId === _.id)
      .filter(_._2.id like s"%$studentId%")
      .filter(_._2.firstName like s"%$studentName%")
      .filter(_._2.lastName like s"%$studentName%")
      .filter(_._2.major like s"%$major%")
      .filter(_._1._2.paymentId.isDefined)
      .groupBy(p => (p._1._1.map(_._1.id), p._1._2.id, p._2.id, p._2.firstName, p._2.lastName, p._2.major))
      .map {
        case (base, group) =>
          val baseOvertimeFine = group.map(_._1._1.map(_._1.overtimeFine.getOrElse(0))).max
          val baseDefectFine = group.map(_._1._1.map(_._1.defectFine.getOrElse(0))).max
          val subOvertimeFine =
            group.map(p => p._1._1.flatMap(_._2.overtimeFine)).sum.getOrElse(0)
          val subDefectFine =
            group.map(p => p._1._1.flatMap(_._2.defectFine)).sum.getOrElse(0)

          (base._1, base._2, base._3, base._4, base._5, base._6, baseOvertimeFine + subOvertimeFine, baseDefectFine + subDefectFine)
      }.result

    db.run(action)
  }

  def getPayment(id: String): Future[Option[(History, Option[Payment])]] = {
    val action = history.joinLeft(payment)
      .on(_.paymentId === _.id)
      .filter(_._1.paymentId === id)
      .result.headOption

    db.run(action)
  }

  def getLastStudentStatus(studentId: String) = {
    val action = history.joinLeft(
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

            (id, baseOvertimeFine + subOvertimeFine, baseDefectFine + subDefectFine)
          }
        ).on(_.paymentId === _._1)
        .join(student).on(_._1.studentId === _.id)
        .filter(_._2.id like s"%$studentId%")
        .sortBy(_._1._1.updatedAt)
        .result.headOption

    db.run(action).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }
}
