package repositories

import javax.inject.{Inject, Singleton}
import models.Student
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

trait StudentComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import  profile.api._

  class Students(tag: Tag) extends Table[Student](tag, "students") {
    def id = column[String]("Id", O.PrimaryKey)
    def firstName = column[String]("FirstName")
    def lastName = column[String]("LastName")
    def major = column[String]("Major")
    def profilePicture = column[String]("ProfilePicture")

    def * = (id, firstName, lastName, major, profilePicture) <> ((Student.apply _).tupled, Student.unapply)
  }
}

@Singleton
class StudentRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {

}
