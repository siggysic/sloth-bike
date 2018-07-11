package repositories

import java.sql.Timestamp
import java.util.UUID

import javax.inject.{Inject, Singleton}
import models.Payment
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait PaymentComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class Payments(tag: Tag) extends Table[Payment](tag, "payments") {
    def id = column[UUID]("Id", O.PrimaryKey)
    def overtimeFine = column[Option[Int]]("OvertimeFine")
    def defectFine = column[Option[Int]]("DefectFine")
    def note = column[String]("Note")
    def updatedAt = column[Timestamp]("UpdatedAt")
    def createdAt = column[Timestamp]("CreatedAt")
    def parentId = column[Option[UUID]]("ParentId")

    def * =
      (id, overtimeFine, defectFine, note, createdAt, updatedAt, parentId) <> ((Payment.apply _).tupled, Payment.unapply)

    def payment =
      foreignKey("payment", parentId, TableQuery[Payments])(_.id.?, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
  }
}

@Singleton()
class PaymentRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] with PaymentComponent {

  import profile.api._

  private val payment = TableQuery[Payments]

  def getPayments(studentId: String): Future[Seq[(UUID, Option[Int], Option[Int])]] = {
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
}
