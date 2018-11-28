package repositories

import javax.inject.{Inject, Singleton}
import model.Facalty
import models.{DBException}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait FacaltyComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._
  class Facalties(tag: Tag) extends Table[Facalty](tag, "facalties") {
    def id = column[Int]("Id", O.PrimaryKey, O.AutoInc)
    def code = column[String]("code")
    def name = column[String]("Name")

    def * = (id.?, code, name).shaped <> ((Facalty.apply _).tupled, Facalty.unapply)
  }
}

@Singleton()
class FacaltyRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends FacaltyComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val facalties = TableQuery[Facalties]

  def getFacalties: Future[Either[DBException.type, Seq[Facalty]]] = {
    val action = facalties.result
    db.run(action).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

  def getFacalty(id: Int): Future[Either[DBException.type, Option[Facalty]]] = {
    val action = facalties.filter(_.id === id).result.headOption
    db.run(action).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }
}
