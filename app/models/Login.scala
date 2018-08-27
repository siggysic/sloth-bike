package models

case class Login(username: String, password: String)

case class LoginFields(username: String = "username", password: String = "password")

case class GraphLogin(available: Int, outOfOrder: Int, borrowOneDay: Int, borrowMoreThanOne: Int)