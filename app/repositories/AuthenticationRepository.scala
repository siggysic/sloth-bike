package repositories

import javax.inject.{Inject, Singleton}
import models.{Authentication, DBException, Login}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthenticationComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import  profile.api._

  class Authentications(tag: Tag) extends Table[Authentication](tag, "students") {
    def id = column[String]("Id", O.PrimaryKey)
    def username = column[String]("Username")
    def password = column[String]("Password")
    def status = column[String]("Status")
    def role = column[String]("Role")

    def * = (id, username, password, status, role) <> ((Authentication.apply _).tupled, Authentication.unapply)
  }
}

@Singleton
class AuthenticationRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends AuthenticationComponent with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val authen = TableQuery[Authentications]

  def login(login: Login): Future[Either[DBException.type, Option[Authentication]]] = {
    val action = authen.filter(a => a.username === login.username && a.password === login.password)

    db.run(action.result.headOption).map(Right.apply).recover {
      case _: Exception => Left(DBException)
    }
  }

}
