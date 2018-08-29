package repositories

import javax.inject.{Inject, Singleton}
import models.{DBException, Student}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait StudentComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import  profile.api._

  class Students(tag: Tag) extends Table[Student](tag, "students") {
    def id = column[String]("Id", O.PrimaryKey)
    def firstName = column[String]("FirstName")
    def lastName = column[String]("LastName")
    def major = column[String]("Major")
    def profilePicture = column[String]("ProfilePicture")

    def * = (id, firstName, lastName, major, profilePicture).shaped <> ((Student.apply _).tupled, Student.unapply)
  }
}

@Singleton
class StudentRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends StudentComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val students = TableQuery[Students]

  def getStudentById(id: String): Future[Either[DBException.type, Option[Student]]] = {
    val action = students.filter(_.id === id).result.headOption
    db.run(action).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

  def getMajors = {
    val action = students.groupBy(_.major).map(_._1).result
    db.run(action).map(Right.apply).recover {
      case _: Exception => Left(exceptions.DBException)
    }
  }

}
