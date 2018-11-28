package repositories

import javax.inject.{Inject, Singleton}
import models.{DBException, Faculty}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait FacultyComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._
  class Faculties(tag: Tag) extends Table[Faculty](tag, "faculties") {
    def id = column[Int]("Id", O.PrimaryKey, O.AutoInc)
    def code = column[String]("code")
    def name = column[String]("Name")

    def * = (id.?, code, name).shaped <> ((Faculty.apply _).tupled, Faculty.unapply)
  }
}

@Singleton()
class FacultyRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends FacultyComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val facalties = TableQuery[Faculties]

  def getFaculties: Future[Either[DBException.type, Seq[Faculty]]] = {
    val action = facalties.result
    db.run(action).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

  def getFaculty(id: Int): Future[Either[DBException.type, Option[Faculty]]] = {
    val action = facalties.filter(_.id === id).result.headOption
    db.run(action).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }
}
