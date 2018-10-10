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
    def phone = column[String]("Phone")
    def major = column[Option[String]]("Major")
    def `type` = column[String]("Type")
    def status = column[String]("Status")
    def address = column[Option[String]]("Address")
    def department = column[Option[String]]("Department")
    def profilePicture = column[Option[String]]("ProfilePicture")

    def * = (id, firstName, lastName, phone, major, `type`, status, address, department, profilePicture).shaped <> ((Student.apply _).tupled, Student.unapply)
  }
}

@Singleton
class StudentRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends StudentComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val students = TableQuery[Students]

  def create(student: Student): Future[Int] = {
    val action = students.insertOrUpdate(student)
    db.run(action)
  }

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
