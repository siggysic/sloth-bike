package repositories

import java.sql.Timestamp

import exceptions.{DBException, CustomException}
import javax.inject.{Inject, Singleton}
import models.{History, Payment, Student}
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
      (id, overtimeFine, defectFine, note.?, createdAt, updatedAt, parentId).shaped <> ((Payment.apply _).tupled, Payment.unapply)

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

  def getPayments(paymentId: String): Future[Option[(String, Option[Int], Option[Int])]] = {
    val action =
      payment.join(payment).on(_.id === _.parentId)
      .filter(_._1.id === paymentId)
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
      }.result.headOption

    db.run(action)
  }

  def getPaymentById(id: String): Future[Either[CustomException, Seq[Payment]]] = {
    val action = payment.filter(p => p.id === id || p.parentId === id).result
    db.run(action).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

  def create(payment: Payment) = {
    val action = this.payment += payment
    db.run(action)
  }

   def createRecover(payment: Payment) = {
    val action = this.payment += payment
    db.run(action).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

  def getFullPayment(studentId: String, studentFirstName: String, studentLastName: String, major: String, page: Int = 1, pageSize: Int = 10) = {
    val action = payment
      .joinLeft(payment).on(_.id === _.parentId)
      .joinRight(history).on(_._1.id === _.paymentId)
      .join(student).on(_._2.studentId === _.id)
      .filter(_._2.id like s"%$studentId%")
      .filter(_._2.firstName like s"%$studentFirstName%")
      .filter(_._2.lastName like s"%$studentLastName%")
      .filter(_._2.major like s"%$major%")
      .filter(_._1._2.paymentId.isDefined)
      .groupBy(p => (p._1._1.map(_._1.id), p._1._2.id, p._2.id, p._2.firstName, p._2.lastName, p._2.major))
      .map {
        case (base, group) =>
          val baseOvertimeFine = group.map(_._1._1.map(_._1.overtimeFine.getOrElse(0))).max.getOrElse(0)
          val baseDefectFine = group.map(_._1._1.map(_._1.defectFine.getOrElse(0))).max.getOrElse(0)
          val subOvertimeFine =
            group.map(p => p._1._1.flatMap(_._2.flatMap(_.overtimeFine).getOrElse(0))).sum.getOrElse(0)
          val subDefectFine =
            group.map(p => p._1._1.flatMap(_._2.flatMap(_.defectFine).getOrElse(0))).sum.getOrElse(0)

          (base._1, base._2, base._3, base._4, base._5, base._6, baseOvertimeFine + subOvertimeFine, baseDefectFine + subDefectFine)
      }

    val total = action.sortBy(_._1).length.result
    val summary = action.drop((page - 1) * pageSize).take(pageSize).result
    db.run(summary).flatMap(s => db.run(total).map(total => (s, total))).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

  def getAllFullPayment(studentId: String, studentFirstName: String, studentLastName: String, major: String) = {
    val action = payment
      .joinLeft(payment).on(_.id === _.parentId)
      .joinRight(history).on(_._1.id === _.paymentId)
      .join(student).on(_._2.studentId === _.id)
      .filter(_._2.id like s"%$studentId%")
      .filter(_._2.firstName like s"%$studentFirstName%")
      .filter(_._2.lastName like s"%$studentLastName%")
      .filter(_._2.major like s"%$major%")
      .filter(_._1._2.paymentId.isDefined)
      .groupBy(p => (p._1._1.map(_._1.id), p._1._2.id, p._2.id, p._2.firstName, p._2.lastName, p._2.major))
      .map {
        case (base, group) =>
          val baseOvertimeFine = group.map(_._1._1.map(_._1.overtimeFine.getOrElse(0))).max.getOrElse(0)
          val baseDefectFine = group.map(_._1._1.map(_._1.defectFine.getOrElse(0))).max.getOrElse(0)
          val subOvertimeFine =
            group.map(p => p._1._1.flatMap(_._2.flatMap(_.overtimeFine).getOrElse(0))).sum.getOrElse(0)
          val subDefectFine =
            group.map(p => p._1._1.flatMap(_._2.flatMap(_.defectFine).getOrElse(0))).sum.getOrElse(0)

          (base._1, base._2, base._3, base._4, base._5, base._6, baseOvertimeFine + subOvertimeFine, baseDefectFine + subDefectFine)
      }


    db.run(action.result).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

  def getPayment(id: String): Future[Option[(History, Option[Payment])]] = {
    val action = history.joinLeft(payment)
      .on(_.paymentId === _.id)
      .filter(_._1.paymentId === id)
      .result.headOption

    db.run(action)
  }

  def getLastStudentStatus(studentId: String): Future[Either[DBException.type, Option[((History, Option[(String, Option[Int], Option[Int])]), Student)]]] = {
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
